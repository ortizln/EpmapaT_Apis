package com.erp.sri_files.services;

import com.erp.sri_files.dto.AttachmentDTO;
import com.erp.sri_files.dto.AutorizacionInfo;
import com.erp.sri_files.dto.AutorizacionSriResult;
import com.erp.sri_files.dto.SendMailRequest;
import com.erp.sri_files.domain.sri.SriIntentoComunicacion;
import com.erp.sri_files.models.Factura;
import com.erp.sri_files.repositories.FacturaR;
import com.erp.sri_files.sri.model.ResultadoSri;
import com.erp.sri_files.sri.model.ServicioSri;
import com.erp.sri_files.sri.model.TipoResultadoSri;
import com.erp.sri_files.sri.retry.SriReintentoDecider;
import com.erp.sri_files.sri.retry.SriReintentoDecision;
import com.erp.sri_files.sri.retry.SriRetryPolicy;
import com.erp.sri_files.utils.FirmaComprobantesService;
import com.erp.sri_files.utils.SriAutorizacionAdapter;
import ec.gob.sri.ws.autorizacion.RespuestaComprobante;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.*;
@RequiredArgsConstructor
@Service
@Slf4j
@ConditionalOnProperty(prefix = "sri.legacy-scheduler", name = "enabled", havingValue = "true")
public class EnvioSriBatchService {

    private static final int LOTE = 10;

    private final FacturaR facturaR;
    private final FacturaXmlGeneratorService xmlFacturaService;
    private final FirmaComprobantesService firmaService;
    private final SendXmlToSriService sendXmlToSriService;
    private final XmlToPdfService xmlToPdfService;
    private final MailService mailService;
    private final SriIntentoService sriIntentoService;
    private final com.erp.sri_files.sri.classifier.SriResponseClassifier clasificador;
    private final SriRetryPolicy retryPolicy;
    private final PlatformTransactionManager transactionManager;
    // private final StorageService storageService; // opcional, para guardar XMLs

    /** Identidad del worker para el claim/concurrencia. */
    private String workerId;

    @PostConstruct
    void init() {
        this.workerId = workerIdCfg == null || workerIdCfg.isBlank()
                ? "sri-files-" + UUID.randomUUID()
                : workerIdCfg;
    }

    @Value("${sri.concurrency.worker-id:}")
    private String workerIdCfg;



    // ====== CONFIG desde application.properties ======
    @Value("${sri.firma.modo:XADES_BES}") // XADES_BES o XMLDSIG
    private String modoFirmaCfg;

    @Value("${sri.ambiente:0}") // 0 = deducir del XML, 1 = pruebas, 2 = producción
    private int ambienteForzado;

    @Value("${sri.poll.intentos:10}")
    private int pollIntentos;

    @Value("${sri.poll.delay-ms:4000}")
    private long pollDelayMs;

    @Value("${sri.concurrency.lock-timeout-minutes:15}")
    private int lockTimeoutMinutes;

        public void automatizacionEnvioFacturasElectonicas() {
            long started = System.currentTimeMillis();
            String threadName = Thread.currentThread().getName();
            logTaskStart("automatizacionEnvioFacturasElectonicas", threadName);
            log.info("Ejecutando envio de facturas at={}", LocalDateTime.now());
            try {
                LocalDateTime ahora = LocalDateTime.now();
                // Claim seguro y concurrente: solo filas estado I sin reintento programado en el futuro,
                // bloqueadas a nivel de fila (FOR UPDATE SKIP LOCKED).
                var facturas = facturaR.reclamarLoteParaProcesar("I", ahora, retryPolicy.getMaxAttempts(), LOTE);

                if (facturas.isEmpty()) {
                    log.info("No hay facturas pendientes estado=I");
                    logTaskEnd("automatizacionEnvioFacturasElectonicas", threadName, started, 0, null);
                    return;
                }

                Metricas m = new Metricas();
                for (Factura f : facturas) {
                    long t0 = System.currentTimeMillis();
                    StringBuilder estados = new StringBuilder();
                    try {
                        ResultadoSri res = reclamarYProcesar(f.getIdfactura());
                        if (res == null) {
                            m.omitidas++;
                            m.logFactura(f.getIdfactura(), "OMITIDA", 0);
                        } else {
                            m.acumular(res);
                            m.logFactura(f.getIdfactura(), String.valueOf(res.getTipo()), res.getDuracionMs());
                        }
                    } catch (Exception ex) {
                        m.erroresNoRecuperables++;
                        m.logFactura(f.getIdfactura(), "EXCEPCION", System.currentTimeMillis() - t0);
                        sriIntentoService.registrarIntento(f.getIdfactura(),
                                clasificarExcepcion(ex),
                                "FACTURA", f.getClaveacceso(), ambienteTexto(),
                                ServicioSri.RECEPCION.name(), null, workerId,
                                sriIntentoService.nuevoCorrelationId(), LocalDateTime.now());
                        log.error("Error procesando factura idfactura={}", f.getIdfactura(), ex);
                    }
                }

                m.logResumen();
                log.info("Lote procesado total={}", facturas.size());
                logTaskEnd("automatizacionEnvioFacturasElectonicas", threadName, started, facturas.size(), m);
            } catch (Exception e) {
                logTaskError("automatizacionEnvioFacturasElectonicas", threadName, e);
                log.error("Error en la tarea programada automatizacionEnvioFacturasElectonicas", e);
            }
        }
    // Dominios que NO quieres que reciban correos
    private static final Set<String> DOMINIOS_BLOQUEADOS = Set.of(
            "epmapatulcan.gob.ec",
            "yimail.com",
            "gamil.com",
            "yahoo.com.mx",
            "hotail.com",
            "homail.com",
            "outloock.com",
            "gmaill.com",
            "hotamil.es",
            "hotmail.com.ar",
            "yahoo.com.ar",
            "YAHOO.COM"
            // agrega más: "hotmail.com", "yahoo.com", etc.
    );
// Consulta los comprobantes en estado C/O para recuperar el XML autorizado.
    public void automatizacionConsultarXml() {
        long started = System.currentTimeMillis();
        String threadName = Thread.currentThread().getName();
        logTaskStart("automatizacionConsultarXml", threadName);
        log.info("Iniciando consulta de XML pendientes");
        try {
            LocalDateTime ahora = LocalDateTime.now();
            // Claim seguro y concurrente (state C y O), corto y sin transacción de lote.
            var facturasReclamadas = new ArrayList<Factura>();
            facturasReclamadas.addAll(facturaR.reclamarLoteParaProcesar("C", ahora, retryPolicy.getMaxAttempts(), LOTE));
            facturasReclamadas.addAll(facturaR.reclamarLoteParaProcesar("O", ahora, retryPolicy.getMaxAttempts(), LOTE));
            Map<Long, Factura> unicas = new LinkedHashMap<>();
            for (Factura factura : facturasReclamadas) {
                unicas.put(factura.getIdfactura(), factura);
            }
            List<Factura> facturas = new ArrayList<>(unicas.values());

            if (facturas.isEmpty()) {
                log.info("No hay facturas pendientes para recuperacion de XML");
                logTaskEnd("automatizacionConsultarXml", threadName, started, 0, null);
                return;
            }

            Metricas m = new Metricas();
            for (Factura f : facturas) {
                long t0 = System.currentTimeMillis();
                try {
                    ResultadoSri res = consultarYRecuperarXml(f);
                    String tipo = res == null ? "OMITIDA" : String.valueOf(res.getTipo());
                    m.acumularNullSafe(res);
                    m.logFactura(f.getIdfactura(), tipo, res == null ? 0 : res.getDuracionMs());
                } catch (Exception ex) {
                    m.erroresNoRecuperables++;
                    m.logFactura(f.getIdfactura(), "EXCEPCION", System.currentTimeMillis() - t0);
                    log.error("Error procesando factura idfactura={}", f.getIdfactura(), ex);
                }
            }
            m.logResumen();
            logTaskEnd("automatizacionConsultarXml", threadName, started, facturas.size(), m);
        } catch (RuntimeException e) {
            logTaskError("automatizacionConsultarXml", threadName, e);
            throw new RuntimeException(e);
        }
    }

    private ResultadoSri consultarYRecuperarXml(Factura f) {
        String claveAcceso = f.getClaveacceso();
        String correlationId = sriIntentoService.nuevoCorrelationId();
        LocalDateTime inicioIntento = LocalDateTime.now();

        if (f.getXmlautorizado() != null && !f.getXmlautorizado().isBlank()) {
            if ("O".equalsIgnoreCase(safeStr(f.getEstado()))) {
                reintentarEnvioCorreoFactura(f);
            } else {
                f.setEstado("A");
                f.setErrores(null);
                facturaR.save(f);
            }
            return ResultadoSri.builder()
                    .tipo(TipoResultadoSri.AUTORIZADA)
                    .servicio(ServicioSri.RECUPERACION.name())
                    .build();
        }

        ResultadoSri ultimo = null;
        int intentos = Math.max(pollIntentos, 1);
        for (int i = 1; i <= intentos; i++) {
            ultimo = sendXmlToSriService.consultarAutorizacionConResultado(claveAcceso);
            sriIntentoService.registrarIntento(f.getIdfactura(), ultimo, "FACTURA", claveAcceso,
                    ambienteTexto(), ServicioSri.AUTORIZACION.name(), null, workerId, correlationId, inicioIntento);
            if (ultimo.getTipo() != TipoResultadoSri.SIN_RESPUESTA) {
                break;
            }
            if (i < intentos) {
                try {
                    Thread.sleep(pollDelayMs);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }

        if (ultimo == null) return null;
        return aplicarEstadoConsultado(f, ultimo);
    }

    /**
     * Aplica a la factura el estado que el resultado de una CONSULTA de
     * autorización determina: AUTORIZADA -&gt; A (+XML), NO_AUTORIZADA -&gt; N,
     * cualquier otra cosa -&gt; se queda en C (pendiente, reintento gateado).
     */
    private ResultadoSri aplicarEstadoConsultado(Factura f, ResultadoSri ultimo) {
        switch (ultimo.getTipo()) {
            case AUTORIZADA:
                f.setXmlautorizado(ultimo.getXmlAutorizado());
                f.setEstado("A");
                f.setErrores(null);
                facturaR.save(f);
                log.info("Factura autorizada y XML guardado idfactura={}", f.getIdfactura());
                break;
            case NO_AUTORIZADA:
                f.setEstado("N");
                f.setErrores(trunc(ultimo.getMensaje(), 1500));
                facturaR.save(f);
                log.warn("Factura no autorizada idfactura={} motivo={}", f.getIdfactura(), ultimo.getMensaje());
                break;
            default:
                // Pendiente o transitorio: sigue en C; el reintento queda programado en la bitácora.
                f.setEstado("C");
                f.setErrores(trunc(mensaje(ultimo), 1500));
                facturaR.save(f);
                log.info("Factura sigue pendiente idfactura={} tipo={}", f.getIdfactura(), ultimo.getTipo());
                break;
        }
        return ultimo;
    }

    /**
     * Recupera bloqueos abandonados (estado P con proceso inactivo): consulta la
     * autorización; si el comprobante ya fue autorizado lo cierra, si NO AUTORIZADO
     * lo marca, y si sigue pendiente lo devuelve a I (el reintento queda gated por la
     * fecha_proximo_intento registrada en sri_intento_comunicacion).
     */
    public void recuperarBloqueosAbandonados() {
        long started = System.currentTimeMillis();
        String threadName = Thread.currentThread().getName();
        logTaskStart("recuperarBloqueosAbandonados", threadName);
        try {
            LocalDateTime umbral = LocalDateTime.now().minusMinutes(lockTimeoutMinutes);
            var bloqueados = facturaR.reclamarBloqueosAbandonados("P", umbral, retryPolicy.getMaxAttempts(), LOTE);

            if (bloqueados.isEmpty()) {
                log.info("No hay bloqueos abandonados que recuperar");
                logTaskEnd("recuperarBloqueosAbandonados", threadName, started, 0, null);
                return;
            }

            Metricas m = new Metricas();
            for (Factura f : bloqueados) {
                long t0 = System.currentTimeMillis();
                try {
                    String clave = f.getClaveacceso();
                    String correlationId = sriIntentoService.nuevoCorrelationId();
                    LocalDateTime inicioIntento = LocalDateTime.now();
                    ResultadoSri ultimo = null;
                    for (int i = 1; i <= Math.max(pollIntentos, 1); i++) {
                        ultimo = sendXmlToSriService.consultarAutorizacionConResultado(clave);
                        sriIntentoService.registrarIntento(f.getIdfactura(), ultimo, "FACTURA", clave,
                                ambienteTexto(), ServicioSri.RECUPERACION.name(), null, workerId,
                                correlationId, inicioIntento);
                        if (ultimo.getTipo() != TipoResultadoSri.SIN_RESPUESTA) break;
                        if (i < Math.max(pollIntentos, 1)) {
                            try {
                                Thread.sleep(pollDelayMs);
                            } catch (InterruptedException ie) {
                                Thread.currentThread().interrupt();
                                break;
                            }
                        }
                    }

                    if (ultimo == null) {
                        m.erroresNoRecuperables++;
                        continue;
                    }
                    Factura actual = facturaR.findById(f.getIdfactura()).orElse(null);
                    if (actual == null) continue;

                    switch (ultimo.getTipo()) {
                        case AUTORIZADA:
                            actual.setXmlautorizado(ultimo.getXmlAutorizado());
                            actual.setEstado("A");
                            actual.setErrores(null);
                            facturaR.save(actual);
                            log.info("Bloqueo recuperado: autorizado idfactura={}", f.getIdfactura());
                            break;
                        case NO_AUTORIZADA:
                            actual.setEstado("N");
                            actual.setErrores(trunc(ultimo.getMensaje(), 1500));
                            facturaR.save(actual);
                            log.warn("Bloqueo recuperado: no autorizado idfactura={}", f.getIdfactura());
                            break;
                        default:
                            // Sigue sin autorización: volver a I pero con reintento gated.
                            actual.setEstado("I");
                            actual.setErrores(trunc(mensaje(ultimo), 1500));
                            facturaR.save(actual);
                            log.info("Bloqueo recuperado: sin autorización aun, reenviar gated idfactura={} tipo={}",
                                    f.getIdfactura(), ultimo.getTipo());
                            break;
                    }
                    m.acumular(ultimo);
                } catch (Exception ex) {
                    m.erroresNoRecuperables++;
                    m.logFactura(f.getIdfactura(), "EXCEPCION", System.currentTimeMillis() - t0);
                    log.error("Error recuperando bloqueo idfactura={}", f.getIdfactura(), ex);
                }
            }
            m.logResumen();
            logTaskEnd("recuperarBloqueosAbandonados", threadName, started, bloqueados.size(), m);
        } catch (Exception e) {
            logTaskError("recuperarBloqueosAbandonados", threadName, e);
            log.error("Error en la tarea programada recuperarBloqueosAbandonados", e);
        }
    }
    /**
     * Motor de reintentos programados (desacoplado del ciclo de envío):
     * procesa los documentos cuyo reintento ya venció (fecha_proximo_intento &lt;= ahora)
     * consultando la autorización con UN SOLO intento por ciclo; el siguiente reintento
     * queda gateado por la nueva fecha_proximo_intento que registra la bitácora.
     *
     * Además cierra los documentos que agotaron el máximo de intentos automáticos:
     * los marca como NO reintentables y los pasa a estado M (revisión manual), de
     * manera que los schedulers de envío/consulta ya no los vuelven a reclamar.
     */
    public void automatizacionReintentosProgramados() {
        if (!retryPolicy.isEnabled()) {
            log.info("Motor de reintentos deshabilitado (sri.retry.enabled=false)");
            return;
        }
        long started = System.currentTimeMillis();
        String threadName = Thread.currentThread().getName();
        logTaskStart("automatizacionReintentosProgramados", threadName);
        Metricas m = new Metricas();
        int cerrados = 0;
        int consultados = 0;
        int saltados = 0;
        try {
            LocalDateTime ahora = LocalDateTime.now();
            int maxIntentos = retryPolicy.getMaxAttempts();

            // 1) Cerrar documentos que agotaron el máximo de intentos automáticos.
            for (SriIntentoComunicacion intento : sriIntentoService.reintentosAgotados(maxIntentos)) {
                Long id = intento.getDocumentoId();
                Factura f = facturaR.findById(id).orElse(null);
                sriIntentoService.marcarNoReintentable(id);
                if (f != null && Arrays.asList("I", "C", "O").contains(safeStr(f.getEstado()).toUpperCase())) {
                    f.setEstado("M");
                    f.setErrores(trunc("Reintentos agotados: " + maxIntentos
                            + " intentos automáticos sin resultado definitivo.", 1500));
                    facturaR.save(f);
                }
                log.warn("[SRI][REINTENTO] Documento idfactura={} agotó {} intentos -> cierre manual (M)",
                        id, maxIntentos);
                cerrados++;
            }

            // 2) Reintentos vencidos con cupo disponible: un solo intento de consulta por ciclo.
            for (SriIntentoComunicacion intento : sriIntentoService.reintentosVencidos(ahora)) {
                Long id = intento.getDocumentoId();
                SriReintentoDecision decision = SriReintentoDecider.decidir(
                        intento.isReintentable(), intento.getNumeroIntento(), maxIntentos,
                        intento.getFechaProximoIntento(), ahora);
                if (decision != SriReintentoDecision.CONSULTAR) {
                    if (decision == SriReintentoDecision.AGOTADO) {
                        cerrados++;
                        sriIntentoService.marcarNoReintentable(id);
                    }
                    continue;
                }
                if (consultados >= LOTE * 2) {
                    break;
                }
                Factura f = facturaR.findById(id).orElse(null);
                if (f == null || "P".equalsIgnoreCase(safeStr(f.getEstado()))) {
                    saltados++;
                    continue;
                }
                try {
                    long t0 = System.currentTimeMillis();
                    ResultadoSri res = sendXmlToSriService.consultarAutorizacionConResultado(f.getClaveacceso());
                    sriIntentoService.registrarIntento(id, res, "FACTURA", f.getClaveacceso(),
                            ambienteTexto(), ServicioSri.AUTORIZACION.name(), "reintentos", workerId,
                            sriIntentoService.nuevoCorrelationId(), LocalDateTime.now());
                    aplicarEstadoConsultado(f, res);
                    m.acumularNullSafe(res);
                    m.logFactura(id, res == null ? "SIN_RESULTADO" : String.valueOf(res.getTipo()),
                            System.currentTimeMillis() - t0);
                    consultados++;
                } catch (Exception ex) {
                    m.erroresNoRecuperables++;
                    log.error("Error consultando reintento idfactura={}", id, ex);
                }
            }

            log.info("[SRI][REINTENTO] cerrados={} consultados={} saltados={}",
                    cerrados, consultados, saltados);
            m.logResumen();
            logTaskEnd("automatizacionReintentosProgramados", threadName, started, consultados + cerrados, m);
        } catch (Exception e) {
            logTaskError("automatizacionReintentosProgramados", threadName, e);
            log.error("Error en la tarea programada automatizacionReintentosProgramados", e);
        }
    }

    private boolean esCorreoPermitido(String email) {
        if (email == null) return false;

        String e = email.trim();
        if (e.isEmpty()) return false;

        // Normalizamos en minúsculas
        e = e.toLowerCase(Locale.ROOT);

        int atIndex = e.lastIndexOf('@');
        if (atIndex <= 0 || atIndex == e.length() - 1) {
            // no tiene @ o está mal formado
            return false;
        }

        String dominio = e.substring(atIndex + 1); // todo lo que está después del @

        // Si el dominio está en la lista negra → NO permitir
        if (DOMINIOS_BLOQUEADOS.contains(dominio)) {
            return false;
        }

        // aquí podrías meter más validaciones de formato si quieres
        return true;
    }





    public ResultadoSri procesarFacturaEnNuevaTx(Long idFactura) {
        String correlationId = sriIntentoService.nuevoCorrelationId();
        LocalDateTime inicioIntento = LocalDateTime.now();
        // Releer la factura ya reclamada (estado P). El claim (I->P) se ejecutó en su
        // propia transacción corta: aquí NO se mantiene una transacción abierta durante
        // las llamadas SOAP. Cada persistencia usa turnos cortos por repositorio.
        Factura f = facturaR.findById(idFactura).orElseThrow();
        if (!"P".equals(f.getEstado())) {
            log.info("Factura ya no esta en estado P idfactura={} estadoActual={}", idFactura, f.getEstado());
            return null;
        }

        try {
            // 2) Generar XML desde la entidad
            String xmlPlano = xmlFacturaService.generarXmlFactura(f);
            requireNotBlank(xmlPlano, "XML generado vacío");

            // 3) Modo de firma
            var mf = "XMLDSIG".equalsIgnoreCase(modoFirmaCfg)
                    ? FirmaComprobantesService.ModoFirma.XMLDSIG
                    : FirmaComprobantesService.ModoFirma.XADES_BES;

            // 4) Firmar
            String xmlFirmado = firmaService.firmarFactura(xmlPlano, mf);
            requireNotBlank(xmlFirmado, "XML firmado vacío");

            // 5) Ambiente
            if (ambienteForzado == 1 || ambienteForzado == 2) {
                sendXmlToSriService.setAmbiente(ambienteForzado);
            } else {
                sendXmlToSriService.setAmbienteFromXml(xmlFirmado);
            }

            // 6) Enviar a recepción (clasificado; nunca lanza por errores de red)
            ResultadoSri resRecepcion = sendXmlToSriService.enviarComprobanteConResultado(xmlFirmado);
            sriIntentoService.registrarIntento(idFactura, resRecepcion, "FACTURA", f.getClaveacceso(),
                    ambienteTexto(), ServicioSri.RECEPCION.name(), null, workerId, correlationId, inicioIntento);

            ResultadoSri retorno = resRecepcion;
            if (resRecepcion.getTipo() == TipoResultadoSri.RECIBIDA) {
                // 7) Polling de autorización
                var rc = sendXmlToSriService.consultar_AutorizacionConEspera(
                        xmlFirmado,
                        clave -> {
                            try {
                                return sendXmlToSriService.consultar_Autorizacion(clave);
                            } catch (Exception e) {
                                throw new RuntimeException(e);
                            }
                        },
                        pollIntentos,
                        pollDelayMs
                );


                var info = SriAutorizacionAdapter.from_Resultado(rc)
                        .orElse(new AutorizacionInfo(false, null, null, null, "Sin autorizaciones"));

                if (info.autorizado()) {

                    // ============================
                    // AUTORIZADA: armar XML completo + guardar fecha/hora
                    // ============================

                    // XML del comprobante (factura) que devuelve el adapter
                    String xmlFactura = new String(info.xmlAutorizado(), StandardCharsets.UTF_8);

                    // Datos de autorización
                    String numeroAutorizacion = info.numeroAutorizacion() != null
                            ? info.numeroAutorizacion().trim()
                            : "";

                    // Dependiendo de cómo manejes la fecha en AutorizacionInfo:
                    // Suponiendo que es XMLGregorianCalendar:
                    LocalDateTime fa = info.fechaAutorizacion();
                    String fechaAutStr = (fa != null ? fa.toString() : "");

                    // Ambiente real → tomado de tu clase
                    String ambienteStr = (ambienteForzado == 2 ? "PRODUCCIÓN" : "PRUEBAS");

                    // Armar XML COMPLETO de autorización
                    String xmlAutorizacionCompleta =
                            "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                                    "<autorizacion>\n" +
                                    "  <estado>AUTORIZADO</estado>\n" +
                                    "  <numeroAutorizacion>" + numeroAutorizacion + "</numeroAutorizacion>\n" +
                                    "  <fechaAutorizacion>" + fechaAutStr + "</fechaAutorizacion>\n" +
                                    "  <ambiente>" + ambienteStr + "</ambiente>\n" +
                                    "  <comprobante><![CDATA[" + xmlFactura + "]]></comprobante>\n" +
                                    "</autorizacion>";
                    // ==========================
                    // ✅ AUTORIZADO
                    // ==========================
                    f.setEstado("A");

                    String xmlAutorizado = new String(info.xmlAutorizado(), StandardCharsets.UTF_8);
                    f.setXmlautorizado(xmlAutorizacionCompleta);   // guardar XML legible

                    // ---------- 1) Generar PDF ----------
                    ByteArrayOutputStream pdfStream = xmlToPdfService.generarFacturaPDF_v3(xmlAutorizacionCompleta);
                    if (pdfStream == null || pdfStream.size() == 0) {
                        // si falla PDF, deja al menos la factura autorizada guardada
                        f.setErrores("Factura autorizada, pero error generando PDF");
                        f.setEstado("O");
                        facturaR.save(f);
                        retorno = ResultadoSri.builder()
                                .tipo(TipoResultadoSri.AUTORIZADA)
                                .mensaje("AUTORIZADO pero error generando PDF")
                                .servicio(ServicioSri.AUTORIZACION.name())
                                .build();
                        return retorno;
                    }

                    byte[] pdfBytes = pdfStream.toByteArray();
                    byte[] xmlBytes = xmlAutorizacionCompleta.getBytes(StandardCharsets.UTF_8);

                    String pdfBase64 = Base64.getEncoder().encodeToString(pdfBytes);
                    String xmlBase64 = Base64.getEncoder().encodeToString(xmlBytes);

                    String nombrePdf = "factura_" + f.getIdfactura() + ".pdf";
                    String nombreXml = "factura_" + f.getIdfactura() + ".xml";

                    List<AttachmentDTO> attachments = List.of(
                            new AttachmentDTO(nombrePdf, "application/pdf", pdfBase64),
                            new AttachmentDTO(nombreXml, "application/xml", xmlBase64)
                    );

                    // ---------- 2) Destinatarios ----------
                    List<String> to = Collections.emptyList(); // por defecto: NO enviar a nadie
                    boolean enviarCorreo = false;

                    String correoComprador = f.getEmailcomprador();

                    if (esCorreoPermitido(correoComprador)) {
                        // Correo válido y permitido → enviar
                        to = List.of(correoComprador.trim());
                        enviarCorreo = true;
                    } else {
                        // Correo no permitido → NO enviar
                        enviarCorreo = false;

                        // IMPORTANTE: simplemente seguimos procesando la factura sin enviar correo
                        log.warn("Correo bloqueado o invalido correo={} idfactura={} accion=no_enviar_correo",
                                correoComprador, f.getIdfactura());
                    }

                    List<String> cc  = Collections.emptyList();
                    List<String> bcc = Collections.emptyList();
                    String from = null; // usa el app.mail.from


                    // ---------- 3) Asunto y cuerpo ----------
                    String subject = "Factura electrónica #"
                            + safeStr(f.getEstablecimiento()) + "-"
                            + safeStr(f.getPuntoemision()) + "-"
                            + safeStr(f.getSecuencial());

                    String htmlBody = "<div style='font-family: Arial, sans-serif; color: #333; max-width: 600px; margin: auto; border: 1px solid #ddd; padding: 20px; border-radius: 8px; background: #f9f9f9;'>" +

                            // Logo
                            "<div style='text-align: center; margin-bottom: 20px; padding-bottom: 20px; border-bottom: 2px solid #0b5394;'>" +
                            "   <img src='https://epmapatulcan.gob.ec/wp/wp-content/uploads/2021/05/LOGO-HORIZONTAL.png' alt='EPMAPA-T' style='max-width: 200px;'/>" +
                            "</div>" +

                            // Título principal
                            "<h2 style='color: #0b5394; text-align: center; margin-bottom: 10px;'>Factura Electrónica Autorizada</h2>" +
                            "<p style='text-align: center; color: #666; font-size: 14px; margin-bottom: 25px;'>Comprobante electrónico generado automáticamente</p>" +

                            // Saludo personalizado
                            "<div style='background: #e8f4ff; padding: 15px; border-radius: 5px; margin-bottom: 20px;'>" +
                            "   <p style='margin: 0;'>Estimado/a <strong>" + (f.getRazonsocialcomprador() != null ? f.getRazonsocialcomprador() : "cliente") + "</strong>,</p>" +
                            "</div>" +

                            // Mensaje principal
                            "<p>Le informamos que su comprobante electrónico ha sido <strong>autorizado</strong> por el Servicio de Rentas Internas (SRI) y se encuentra disponible para su descarga.</p>" +

                            // Estado SRI destacado
                            "<div style='background: #f0f8f0; border: 1px solid #4caf50; border-radius: 5px; padding: 12px; margin: 20px 0;'>" +
                            "   <p style='margin: 0; text-align: center;'><strong>Estado SRI:</strong> <span style='color: #2e7d32; font-weight: bold;'>✅ AUTORIZADO</span></p>" +
                            "</div>" +

                            // Información importante
                            "<div style='background: #fff3cd; border: 1px solid #ffc107; border-radius: 5px; padding: 15px; margin: 20px 0;'>" +
                            "   <h4 style='color: #856404; margin-top: 0;'>📎 Documentos Adjuntos</h4>" +
                            "   <p style='margin: 5px 0;'>• <strong>Factura en formato PDF</strong> - Documento legible</p>" +
                            "   <p style='margin: 5px 0;'>• <strong>Archivo XML</strong> - Comprobante electrónico oficial</p>" +
                            "</div>" +

                            // Separador
                            "<hr style='border: none; border-top: 2px dashed #ccc; margin: 25px 0;'>" +

                            // Información de la empresa
                            "<h3 style='color: #0b5394; border-bottom: 1px solid #0b5394; padding-bottom: 8px;'>Información de Contacto</h3>" +
                            "<div style='line-height: 1.6;'>" +
                            "   <p><strong>🏢 EPMAPA-T</strong><br>" +
                            "   Empresa Pública Municipal de Agua Potable y Alcantarillado de Tulcán</p>" +
                            "   <p><strong>📍 Dirección:</strong> Ca. Juan Ramón Arellano y Bolívar, Tulcán – Ecuador<br>" +
                            "   <strong>🕒 Horario de atención:</strong> Lunes a Viernes 07h30 - 16h30<br>" +
                            "   <strong>📞 Teléfono:</strong> +(593) 06 298 0021<br>" +
                            "   <strong>🌐 Portal web:</strong> <a href='https://epmapatulcan.gob.ec/wp/' target='_blank' style='color: #0b5394;'>epmapatulcan.gob.ec</a></p>" +
                            "</div>" +

                            // Redes sociales
                            "<h4 style='color: #0b5394; margin-top: 25px;'>Síguenos en nuestras redes sociales:</h4>" +
                            "<div style='background: #f8f9fa; padding: 12px; border-radius: 5px;'>" +
                            "   <p style='margin: 5px 0;'>📘 Facebook: <a href='https://www.facebook.com/epmapat2023' target='_blank' style='color: #0b5394;'>facebook.com/epmapat2023</a></p>" +
                            "   <p style='margin: 5px 0;'>📷 Instagram: <a href='https://www.instagram.com/epmapat_/' target='_blank' style='color: #0b5394;'>@epmapat_</a></p>" +
                            "   <p style='margin: 5px 0;'>💬 WhatsApp: <a href='https://api.whatsapp.com/send?phone=593963967739' target='_blank' style='color: #0b5394;'>+593 963967739</a></p>" +
                            "</div>" +

                            // Mensaje importante - NO RESPONDER
                            "<div style='background: #fff3f3; border: 1px solid #dc3545; border-radius: 5px; padding: 15px; margin: 25px 0;'>" +
                            "   <h4 style='color: #dc3545; margin-top: 0; text-align: center;'>⚠️ IMPORTANTE</h4>" +
                            "   <p style='margin: 10px 0; text-align: center; font-weight: bold;'>Este es un mensaje automático, por favor no responda a este correo.</p>" +
                            "   <p style='margin: 10px 0; text-align: center; font-size: 14px;'>Si necesita contactarnos, utilice los canales oficiales mencionados anteriormente.</p>" +
                            "   <p style='margin: 10px 0; text-align: center; font-size: 14px;'>El correo <strong>info@epmapatulcan.gob.ec</strong> no está monitoreado para respuestas.</p>" +
                            "</div>" +

                            // Despedida
                            "<div style='text-align: center; margin-top: 30px; padding-top: 20px; border-top: 1px solid #ddd;'>" +
                            "   <p style='margin: 0; color: #666;'>Gracias por confiar en nuestros servicios</p>" +
                            "   <p style='margin: 10px 0 0 0; font-weight: bold; color: #0b5394;'>Atentamente,<br>EPMAPA-T</p>" +
                            "</div>" +

                            // Pie de página
                            "<div style='text-align: center; margin-top: 20px; padding-top: 15px; border-top: 1px solid #eee; font-size: 12px; color: #999;'>" +
                            "   <p style='margin: 0;'>© " + java.time.Year.now().getValue() + " EPMAPA-T. Todos los derechos reservados.</p>" +
                            "   <p style='margin: 5px 0 0 0;'>Este correo electrónico fue generado automáticamente.</p>" +
                            "</div>" +

                            "</div>";

                    Map<String,String> inlineImages = Collections.emptyMap();

                    SendMailRequest mailReq = new SendMailRequest(
                            from,
                            to,
                            cc,
                            bcc,
                            subject,
                            htmlBody,
                            attachments,
                            inlineImages
                    );

                    try {
                        // si tu MailService tiene send(...) como en el controller:
                        if (enviarCorreo) {
                            try {
                                mailService.send(mailReq);  // solo si enviarCorreo == true
                            } catch (Exception mailEx) {
                                f.setEstado("O");
                                String err = "Error enviando correo: " + mailEx.getMessage();
                                f.setErrores((f.getErrores() == null) ? err : (f.getErrores() + " | " + err));
                            }
                        } else {
                            log.info("Saltando envio de correo idfactura={}", f.getIdfactura());
                        }
                    } catch (Exception mailEx) {
                        // si falla el correo, marca como "O" (opcional)
                        f.setEstado("O");
                        String err = "Error enviando correo: " + mailEx.getMessage();
                        f.setErrores((f.getErrores() == null) ? err : (f.getErrores() + " | " + err));
                    }

                    facturaR.save(f);
                    retorno = ResultadoSri.builder()
                            .tipo(TipoResultadoSri.AUTORIZADA)
                            .mensaje("AUTORIZADO")
                            .servicio(ServicioSri.AUTORIZACION.name())
                            .duracionMs(resRecepcion.getDuracionMs())
                            .build();

                } else {
                    // ==========================
                    // ❌ NO AUTORIZADO
                    // ==========================
                    f.setEstado("N");
                    String xml = new String(info.xmlAutorizado(), StandardCharsets.UTF_8);
                    // Puedes guardar XML de error o solo mensajes:
                    f.setErrores(trunc(info.mensajesConcatenados(), 1500));
                    facturaR.save(f);
                    retorno = ResultadoSri.builder()
                            .tipo(TipoResultadoSri.NO_AUTORIZADA)
                            .mensaje(info.mensajesConcatenados())
                            .servicio(ServicioSri.AUTORIZACION.name())
                            .build();
                }

                return retorno;
            } else {
                // Recepción NO devolvió RECIBIDA: decidir según clasificación.
                switch (resRecepcion.getTipo()) {
                    case CLAVE_REGISTRADA:
                        // No reenviar el comprobante: la clave ya fue registrada.
                        f.setEstado("C"); // pendiente de autorización (se consulta en la tarea de recuperación)
                        f.setErrores(trunc(mensaje(resRecepcion), 1500));
                        facturaR.save(f);
                        log.warn("Factura CLAVE ACCESO REGISTRADA idfactura={} -> estado C (solo consulta autorización)",
                                f.getIdfactura());
                        break;
                    case ERROR_TRANSITORIO:
                    case SIN_RESPUESTA:
                        // Error de red/incierto: estado C con reintento programado
                        // (fecha_proximo_intento ya quedó en sri_intento_comunicacion).
                        f.setEstado("C");
                        f.setErrores(trunc(mensaje(resRecepcion), 1500));
                        facturaR.save(f);
                        log.warn("Factura con comunicacion transitoria idfactura={} tipo={} -> estado C",
                                f.getIdfactura(), resRecepcion.getTipo());
                        break;
                    default:
                        // DEVUELTA u otro resultado definitivo: NO reintentar.
                        f.setEstado("M");
                        f.setErrores(trunc(mensaje(resRecepcion), 1500));
                        facturaR.save(f);
                        log.warn("Factura devuelta por recepcion SRI idfactura={} errores={}",
                                f.getIdfactura(), mensaje(resRecepcion));
                        break;
                }
                return resRecepcion;
            }

        } catch (Exception ex) {
            // Clasificar el error técnico: NUNCA volver a "I" para reenviar ciegamente.
            // Transitorio -> C (reintento programado), funcional -> M.
            log.error("Error inesperado en factura idfactura={}", idFactura, ex);
            ResultadoSri res = clasificarExcepcion(ex);
            sriIntentoService.registrarIntento(idFactura, res, "FACTURA", f.getClaveacceso(),
                    ambienteTexto(), ServicioSri.RECEPCION.name(), null, workerId, correlationId, inicioIntento);
            f.setEstado(res.isReintentable() ? "C" : "M");
            f.setErrores(trunc(res.getExcepcion(), 1500));
            facturaR.save(f);
            return res;
        }
    }

    // helper sencillo, igual al del controller
    private void reintentarEnvioCorreoFactura(Factura f) {
        try {
            String xmlAutorizado = safeStr(f.getXmlautorizado());
            if (xmlAutorizado.isBlank()) {
                f.setEstado("C");
                f.setErrores("No existe XML autorizado para reenviar correo");
                facturaR.save(f);
                return;
            }

            String correoComprador = safeStr(f.getEmailcomprador());
            if (!esCorreoPermitido(correoComprador)) {
                f.setEstado("A");
                f.setErrores(null);
                facturaR.save(f);
                return;
            }

            ByteArrayOutputStream pdfStream = xmlToPdfService.generarFacturaPDF_v3(xmlAutorizado);
            if (pdfStream == null || pdfStream.size() == 0) {
                f.setEstado("O");
                f.setErrores("Factura autorizada, pero no se pudo regenerar el PDF para reenviar correo");
                facturaR.save(f);
                return;
            }

            byte[] pdfBytes = pdfStream.toByteArray();
            byte[] xmlBytes = xmlAutorizado.getBytes(StandardCharsets.UTF_8);

            String pdfBase64 = Base64.getEncoder().encodeToString(pdfBytes);
            String xmlBase64 = Base64.getEncoder().encodeToString(xmlBytes);

            String nombrePdf = "factura_" + f.getIdfactura() + ".pdf";
            String nombreXml = "factura_" + f.getIdfactura() + ".xml";

            List<AttachmentDTO> attachments = List.of(
                    new AttachmentDTO(nombrePdf, "application/pdf", pdfBase64),
                    new AttachmentDTO(nombreXml, "application/xml", xmlBase64)
            );

            List<String> to = List.of(correoComprador.trim());
            List<String> cc = Collections.emptyList();
            List<String> bcc = Collections.emptyList();
            String from = null;

            String subject = "Factura electrónica #"
                    + safeStr(f.getEstablecimiento()) + "-"
                    + safeStr(f.getPuntoemision()) + "-"
                    + safeStr(f.getSecuencial());

            String htmlBody = "<p>Estimado/a " + (f.getRazonsocialcomprador() != null ? f.getRazonsocialcomprador() : "cliente") + ",</p>"
                    + "<p>Su factura electrónica fue autorizada por el SRI. Adjuntamos nuevamente el PDF y el XML.</p>"
                    + "<p><strong>Clave de acceso:</strong> " + safeStr(f.getClaveacceso()) + "</p>"
                    + "<p>Este es un mensaje automático de EPMAPA-T.</p>";

            Map<String, String> inlineImages = Collections.emptyMap();
            SendMailRequest mailReq = new SendMailRequest(
                    from,
                    to,
                    cc,
                    bcc,
                    subject,
                    htmlBody,
                    attachments,
                    inlineImages
            );

            mailService.send(mailReq);
            f.setEstado("A");
            f.setErrores(null);
            facturaR.save(f);
        } catch (Exception ex) {
            f.setEstado("O");
            f.setErrores(trunc("Error reenviando correo: " + ex.getMessage(), 1500));
            facturaR.save(f);
        }
    }

    private String safeStr(Object o) {
        return (o == null) ? "" : o.toString().trim();
    }


    private void logTaskStart(String taskName, String threadName) {
        log.info("[SCHEDULER][{}] INICIO {} @ {}", threadName, taskName, LocalDateTime.now());
    }

    private void logTaskEnd(String taskName, String threadName, long started, int totalDetectado, Metricas m) {
        long durationMs = System.currentTimeMillis() - started;
        if (m == null) {
            log.info("[SCHEDULER][{}] FIN {} | detectados={} | sin_metricas | duracionMs={}",
                    threadName, taskName, totalDetectado, durationMs);
            return;
        }
        log.info("[SCHEDULER][{}] FIN {} | duracionMs={} | {}",
                threadName, taskName, durationMs, m.resumen());
    }

    private void logTaskError(String taskName, String threadName, Exception e) {
        log.error("[SCHEDULER][{}] ERROR {}", threadName, taskName, e);
    }

    // helpers
        private static void requireNotBlank(String s, String msg) {
            if (s == null || s.isBlank()) throw new IllegalStateException(msg);
        }

        private static String trunc(String s, int max) {
            if (s == null) return null;
            return s.length() <= max ? s : s.substring(0, max);
        }

        private static String mensaje(ResultadoSri r) {
            if (r == null || (r.getMensaje() == null || r.getMensaje().isBlank())) return "Sin detalle";
            return r.getMensaje();
        }

        private String ambienteTexto() {
            return sendXmlToSriService.getAmbiente() == 2 ? "PRODUCCION" : "PRUEBAS";
        }

        private ResultadoSri clasificarExcepcion(Exception ex) {
            return clasificador.clasificarExcepcion(ex, ServicioSri.RECEPCION.name(), 0L);
        }

        private TransactionTemplate nuevaTransaccion() {
            TransactionTemplate tt = new TransactionTemplate(transactionManager);
            tt.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
            return tt;
        }

        /** Claim: estado I->P + intento CLAIM en una única transacción corta. */
        private boolean claimFactura(Long idFactura) {
            try {
                return Boolean.TRUE.equals(nuevaTransaccion().execute(status -> {
                    Factura f = facturaR.findById(idFactura).orElse(null);
                    if (f == null || !"I".equals(f.getEstado())) {
                        return false;
                    }
                    f.setEstado("P");
                    facturaR.save(f);
                    sriIntentoService.registrarIntento(idFactura, null, "FACTURA", f.getClaveacceso(),
                            ambienteTexto(), ServicioSri.CLAIM.name(), null, workerId,
                            sriIntentoService.nuevoCorrelationId(), LocalDateTime.now());
                    return true;
                }));
            } catch (Exception ex) {
                log.error("No se pudo reclamar factura idfactura={}", idFactura, ex);
                return false;
            }
        }

        /** Reclama la factura (TX corta) y luego la procesa sin transacción abierta. */
        private ResultadoSri reclamarYProcesar(Long idFactura) {
            if (!claimFactura(idFactura)) {
                return null;
            }
            return procesarFacturaEnNuevaTx(idFactura);
        }

    // ============================================================
    // Métricas reales del lote: procesar NO es sinónimo de exitoso.
    // ============================================================
    private static final class Metricas {
        int detectadas;
        int completadas;
        int recibidasSri;
        int pendientesAutorizacion;
        int autorizadas;
        int noAutorizadas;
        int devueltas;
        int claveRegistrada;
        int erroresTransitorios;
        int erroresNoRecuperables;
        int reintentosProgramados;
        int omitidas;

        void acumular(ResultadoSri res) {
            if (res == null) {
                omitidas++;
                return;
            }
            detectadas++;
            if (res.getTipo() == null) return;
            switch (res.getTipo()) {
                case RECIBIDA -> recibidasSri++;
                case AUTORIZADA -> {
                    autorizadas++;
                    completadas++;
                }
                case NO_AUTORIZADA -> noAutorizadas++;
                case DEVUELTA -> devueltas++;
                case CLAVE_REGISTRADA -> {
                    claveRegistrada++;
                    pendientesAutorizacion++;
                }
                case ERROR_TRANSITORIO -> {
                    erroresTransitorios++;
                    reintentosProgramados++;
                    pendientesAutorizacion++;
                }
                case SIN_RESPUESTA -> {
                    erroresTransitorios++;
                    reintentosProgramados++;
                    pendientesAutorizacion++;
                }
                default -> erroresNoRecuperables++;
            }
        }

        void acumularNullSafe(ResultadoSri res) {
            acumular(res);
        }

        void logFactura(Long idfactura, String resultado, long duracionMs) {
            log.info("[BATCH][FACTURA] idfactura={} | resultado={} | duracionMs={}", idfactura, resultado, duracionMs);
        }

        String resumen() {
            return "detectadas=" + detectadas
                    + " | completadas=" + completadas
                    + " | recibidasSri=" + recibidasSri
                    + " | autorizadas=" + autorizadas
                    + " | noAutorizadas=" + noAutorizadas
                    + " | devueltas=" + devueltas
                    + " | claveRegistrada=" + claveRegistrada
                    + " | pendientesAutorizacion=" + pendientesAutorizacion
                    + " | erroresTransitorios=" + erroresTransitorios
                    + " | erroresNoRecuperables=" + erroresNoRecuperables
                    + " | reintentosProgramados=" + reintentosProgramados
                    + " | omitidas=" + omitidas;
        }

        void logResumen() {
            log.info("[BATCH][RESUMEN] {}", resumen());
        }
    }

}

