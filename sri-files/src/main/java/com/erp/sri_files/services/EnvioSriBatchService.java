package com.erp.sri_files.services;

import com.erp.sri_files.dto.AttachmentDTO;
import com.erp.sri_files.dto.AutorizacionInfo;
import com.erp.sri_files.dto.AutorizacionSriResult;
import com.erp.sri_files.dto.SendMailRequest;
import com.erp.sri_files.models.Factura;
import com.erp.sri_files.repositories.FacturaR;
import com.erp.sri_files.utils.FirmaComprobantesService;
import com.erp.sri_files.utils.SriAutorizacionAdapter;
import ec.gob.sri.ws.autorizacion.RespuestaComprobante;
import ec.gob.sri.ws.recepcion.RespuestaSolicitud;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.annotation.Propagation;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.*;
@RequiredArgsConstructor
@Service
public class EnvioSriBatchService {

    private static final int LOTE = 10;

    private final FacturaR facturaR;
    private final FacturaXmlGeneratorService xmlFacturaService;
    private final FirmaComprobantesService firmaService;
    private final SendXmlToSriService sendXmlToSriService;
    private final XmlToPdfService xmlToPdfService;
    private final MailService mailService;
    // private final StorageService storageService; // opcional, para guardar XMLs



    // ====== CONFIG desde application.properties ======
    @Value("${sri.firma.modo:XADES_BES}") // XADES_BES o XMLDSIG
    private String modoFirmaCfg;

    @Value("${sri.ambiente:0}") // 0 = deducir del XML, 1 = pruebas, 2 = producción
    private int ambienteForzado;

    @Value("${sri.poll.intentos:10}")
    private int pollIntentos;

    @Value("${sri.poll.delay-ms:4000}")
    private long pollDelayMs;

        @Transactional
        @Scheduled(cron = "${interes.tarea.cron}") // ej: 0 */5 * * * *
        public void automatizacionEnvioFacturasElectonicas() {
            System.out.println("⏰ Ejecutando envío de facturas : " + LocalDateTime.now());
            try {
                var limit = PageRequest.of(0, LOTE);
                var page = facturaR.findByEstado("I", limit);
                List<Factura> facturas = page.getContent();

                if (facturas.isEmpty()) {
                    System.out.println("🟢 No hay facturas pendientes (I).");
                    return;
                }

                for (Factura f : facturas) {
                    try {
                        procesarFacturaEnNuevaTx(f.getIdfactura());
                    } catch (Exception ex) {
                        System.err.println("❌ Error procesando factura " + f.getIdfactura() + ": " + ex.getMessage());
                        ex.printStackTrace();
                    }
                }

                System.out.println("✅ Lote procesado: " + facturas.size());
            } catch (Exception e) {
                System.err.println("❌ Error en la tarea programada: " + e.getMessage());
                e.printStackTrace();
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
//Este servicio sirve para conultar las facturas en estado C y volver a buscar el xml auotizado en el sri
    @Transactional
    @Scheduled(cron = "${interes.tarea.cron}")
    public void automatizacionConsultarXml() {
        System.out.println("Voy a consultar cada xml que aun no esta");
        try {
            var limit = PageRequest.of(0, LOTE);
            var page = facturaR.findByEstado("C", limit);
            List<Factura> facturas = page.getContent();

            if (facturas.isEmpty()) {
                System.out.println("🟢 No hay facturas pendientes (I).");
                return;
            }

            for (Factura f : facturas) {
                try {
                    String claveAcceso = f.getClaveacceso();
                    if(f.getXmlautorizado() != null || f.getXmlautorizado() != "")
                    {
                        f.setEstado("A");
                        f.setErrores(null);
                        facturaR.save(f);
                        break;
                    }
                    AutorizacionSriResult resultado =
                            sendXmlToSriService.consultar_Autorizacion(claveAcceso);

                    if (resultado.isAutorizado()
                            && resultado.getXmlAutorizado() != null) {
                        // 👉 Guardas el XML autorizado
                        f.setXmlautorizado(resultado.getXmlAutorizado());

                        // 👉 Campo autorizado (ajusta según tu tipo de dato)
                        // Si es Boolean:
                       // f.set(true);
                        // Si es String tipo 'S'/'N': f.setAutorizado("S");

                        // 👉 Si manejas estado de factura
                        f.setEstado("A"); // Autorizada

                        // 👉 Si tienes fecha de autorización en la entidad
                        if (resultado.getFechaAutorizacion() != null) {
                            f.setErrores(String.valueOf(resultado.getFechaAutorizacion()));
                        }

                        // Limpia errores si los hubiera
                        f.setErrores(null);

                        System.out.println("✅ Factura " + f.getIdfactura()
                                + " autorizada y XML guardado.");
                    } else {
                        // No autorizado, en proceso o error
                        String msg = resultado.getMensaje() != null
                                ? resultado.getMensaje()
                                : "No autorizado / sin XML";

                        f.setEstado("C"); // o "N"
                        f.setErrores(msg);

                        System.out.println("⚠️ Factura " + f.getIdfactura()
                                + " no autorizada: " + msg);
                    }

                    facturaR.save(f);

                } catch (Exception ex) {
                    System.err.println("❌ Error procesando factura "
                            + f.getIdfactura() + ": " + ex.getMessage());
                    ex.printStackTrace();
                }
            }

        } catch (RuntimeException e) {
            throw new RuntimeException(e);
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



    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void procesar_FacturaEnNuevaTx(Long idFactura) {
        // 1) Releer y bloquear lógicamente
        Factura f = facturaR.findById(idFactura).orElseThrow();
        if (!"I".equals(f.getEstado())) {
            System.out.println("ℹ️ Factura " + idFactura + " ya no está en estado I, actual: " + f.getEstado());
            return;
        }
        f.setEstado("P");
        facturaR.saveAndFlush(f);

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

            // 6) Enviar a recepción
            RespuestaSolicitud recepcion = sendXmlToSriService.enviarFacturaFirmadaTxt(xmlFirmado);
            if (recepcion == null) throw new IllegalStateException("Respuesta de recepción nula");

            if ("RECIBIDA".equalsIgnoreCase(recepcion.getEstado())) {
                // 7) Polling de autorización
                var rc = sendXmlToSriService.consultarAutorizacionConEspera(
                        xmlFirmado,
                        clave -> {
                            try {
                                return sendXmlToSriService.consultarAutorizacion(clave);
                            } catch (Exception e) {
                                throw new RuntimeException(e);
                            }
                        },
                        pollIntentos,
                        pollDelayMs
                );

                var info = SriAutorizacionAdapter.fromRespuesta(rc)
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

                    f.setEstado("A");
                    // Guardas el XML COMPLETO
                    f.setXmlautorizado(xmlAutorizacionCompleta);

                    // Si tu entidad tiene estos campos, guárdalos también:
                    // (cambia los nombres de métodos según tu entidad)
                  /*  try {
                        f.setNumeroautorizacion(numeroAutorizacion); // o setNumautorizacion(...)
                    } catch (Exception ignore) {}

                    try {
                        if (fa != null) {
                            // convertir a java.util.Date o LocalDateTime según tu campo
                            java.util.Date fechaJava = fa.toGregorianCalendar().getTime();
                            f.setFechaautorizacion(fechaJava); // ajusta el nombre
                        }
                    } catch (Exception ignore) {}*/

                    facturaR.save(f);

                } else {
                    // ============================
                    // NO AUTORIZADA
                    // ============================
                    f.setEstado("U");

                    // Si quieres ver el XML que devolvió SRI:
                    String xml = new String(info.xmlAutorizado(), StandardCharsets.UTF_8);

                    // Mensajes del SRI
                    String mensajes = info.mensajesConcatenados();

                    // Guarda solo mensajes (lo más útil) y evita sobrescribir dos veces
                    String errores = (mensajes != null && !mensajes.isBlank())
                            ? mensajes
                            : xml;

                    f.setErrores(trunc(errores, 1500));
                    facturaR.save(f);
                }

            } else {
                // ============================
                // DEVUELTA en recepción
                // ============================
                f.setEstado("C"); // tu estado para devuelta/observada
                String errores = erroresRecepcion(recepcion);
                f.setErrores(trunc(errores, 1500));
                facturaR.save(f);
                System.out.println("🟠 DEVUELTA (REC): " + f.getIdfactura() + " -> " + errores);
            }

        } catch (Exception ex) {
            // Error inesperado → devolver a I y aumentar reintentos
            System.err.println("❌ Error inesperado en factura " + idFactura + ": " + ex.getMessage());
            ex.printStackTrace();

            f.setEstado("I");
            facturaR.save(f);
        }
    }


    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void procesarFacturaEnNuevaTx(Long idFactura) {
        // 1) Releer y bloquear lógicamente
        Factura f = facturaR.findById(idFactura).orElseThrow();
        if (!"I".equals(f.getEstado())) {
            System.out.println("ℹ️ Factura " + idFactura + " ya no está en estado I, actual: " + f.getEstado());
            return;
        }
        f.setEstado("P");
        facturaR.saveAndFlush(f);

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

            // 6) Enviar a recepción
            RespuestaSolicitud recepcion = sendXmlToSriService.enviarFacturaFirmadaTxt(xmlFirmado);
            if (recepcion == null) throw new IllegalStateException("Respuesta de recepción nula");

            if ("RECIBIDA".equalsIgnoreCase(recepcion.getEstado())) {
                // 7) Polling de autorización
              /*  var _rc = sendXmlToSriService.consultarAutorizacionConEspera(
                        xmlFirmado,
                        clave -> {
                            try {
                                return sendXmlToSriService.consultarAutorizacion(clave);
                            } catch (Exception e) {
                                throw new RuntimeException(e);
                            }
                        },
                        pollIntentos,
                        pollDelayMs
                );*/

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
                        return;
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
                    //List<String> to;
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
                        System.out.println("⚠️ Correo bloqueado o inválido (" + correoComprador +
                                ") → No se enviará correo para la factura " + f.getIdfactura());
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
                            System.out.println("ℹ️ Saltando correo para factura " + f.getIdfactura());
                        }
                    } catch (Exception mailEx) {
                        // si falla el correo, marca como "O" (opcional)
                        f.setEstado("O");
                        String err = "Error enviando correo: " + mailEx.getMessage();
                        f.setErrores((f.getErrores() == null) ? err : (f.getErrores() + " | " + err));
                    }

                    facturaR.save(f);

                } else {
                    // ==========================
                    // ❌ NO AUTORIZADO
                    // ==========================
                    f.setEstado("N");
                    String xml = new String(info.xmlAutorizado(), StandardCharsets.UTF_8);
                    // Puedes guardar XML de error o solo mensajes:
                    f.setErrores(trunc(info.mensajesConcatenados(), 1500));
                    facturaR.save(f);
                }

            } else {
                // DEVUELTA en recepción
                f.setEstado("M"); // tu estado para devuelta/observada
                String errores = erroresRecepcion(recepcion);
                f.setErrores(trunc(errores, 1500));
                facturaR.save(f);
                System.out.println("🟠 DEVUELTA (REC): " + f.getIdfactura() + " -> " + errores);
            }

        } catch (Exception ex) {
            // Error inesperado → devolver a I y aumentar reintentos
            System.err.println("❌ Error inesperado en factura " + idFactura + ": " + ex.getMessage());
            ex.printStackTrace();

            f.setEstado("I");
            f.setErrores(ex.getMessage());
            facturaR.save(f);
        }
    }

    // helper sencillo, igual al del controller
    private String safeStr(Object o) {
        return (o == null) ? "" : o.toString().trim();
    }


    // helpers
        private static void requireNotBlank(String s, String msg) {
            if (s == null || s.isBlank()) throw new IllegalStateException(msg);
        }

        private static String trunc(String s, int max) {
            if (s == null) return null;
            return s.length() <= max ? s : s.substring(0, max);
        }

    // Extrae mensajes legibles de RespuestaSolicitud (recepción SRI)
    private static String erroresRecepcion(RespuestaSolicitud rs) {
        if (rs == null || rs.getComprobantes() == null || rs.getComprobantes().getComprobante() == null) {
            return "Respuesta de recepción sin detalles de comprobantes";
        }
        StringBuilder sb = new StringBuilder();
        for (var comp : rs.getComprobantes().getComprobante()) {
            String clave = nonNull(comp.getClaveAcceso());
            if (sb.length() > 0) sb.append(" || ");
            sb.append("Clave: ").append(clave.isEmpty() ? "-" : clave);

            if (comp.getMensajes() != null && comp.getMensajes().getMensaje() != null) {
                for (var m : comp.getMensajes().getMensaje()) {
                    sb.append(" | [")
                            .append(nonNull(m.getIdentificador()))
                            .append("] ")
                            .append(nonNull(m.getMensaje()));
                    String ia = nonNull(m.getInformacionAdicional());
                    if (!ia.isEmpty()) sb.append(" (").append(ia).append(")");
                    String tipo = nonNull(m.getTipo()); // ERROR/INFO/ADVERTENCIA
                    if (!tipo.isEmpty()) sb.append(" <").append(tipo).append(">");
                }
            }
        }
        if (sb.length() == 0) return "DEVUELTA sin mensajes";
        return sb.toString();
    }

    private static String nonNull(String s) { return s == null ? "" : s.trim(); }

}
