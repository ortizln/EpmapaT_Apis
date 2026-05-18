package com.erp.sri_files.services;

import com.erp.sri_files.dto.AutorizacionSriResult;
import ec.gob.sri.ws.autorizacion.Autorizacion;
import ec.gob.sri.ws.autorizacion.AutorizacionComprobantesOffline;
import ec.gob.sri.ws.autorizacion.AutorizacionComprobantesOfflineService;
import ec.gob.sri.ws.autorizacion.RespuestaComprobante;
import ec.gob.sri.ws.recepcion.RecepcionComprobantesOffline;
import ec.gob.sri.ws.recepcion.RecepcionComprobantesOfflineService;
import ec.gob.sri.ws.recepcion.RespuestaSolicitud;
import jakarta.annotation.PostConstruct;
import jakarta.xml.ws.BindingProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.xml.namespace.QName;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.text.Normalizer;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;

@Service
public class SendXmlToSriService {

    /** 1 = PRUEBAS, 2 = PRODUCCIÓN */
    private int ambiente = 2;

    // === WSDL locales (classpath) ===
    @Value("${sri.wsdl.local.recepcion:wsdl/RecepcionComprobantesOffline.wsdl}")
    private String wsdlLocalRecepcion;

    @Value("${sri.wsdl.local.autorizacion:wsdl/AutorizacionComprobantesOffline.wsdl}")
    private String wsdlLocalAutorizacion;

    // === Endpoints reales (SIN ?wsdl) ===
    @Value("${sri.endpoint.recepcion.pruebas:https://celcer.sri.gob.ec/comprobantes-electronicos-ws/RecepcionComprobantesOffline}")
    private String epRecepcionPruebas;

    @Value("${sri.endpoint.autorizacion.pruebas:https://celcer.sri.gob.ec/comprobantes-electronicos-ws/AutorizacionComprobantesOffline}")
    private String epAutorizacionPruebas;

    @Value("${sri.endpoint.recepcion.produccion:https://cel.sri.gob.ec/comprobantes-electronicos-ws/RecepcionComprobantesOffline}")
    private String epRecepcionProd;

    @Value("${sri.endpoint.autorizacion.produccion:https://cel.sri.gob.ec/comprobantes-electronicos-ws/AutorizacionComprobantesOffline}")
    private String epAutorizacionProd;

    // Ambiente por properties (1|2)
    @Value("${sri.ambiente:2}")
    public void setAmbiente(int ambiente) { this.ambiente = (ambiente == 2) ? 2 : 1; }
    public int getAmbiente() { return ambiente; }

    @PostConstruct
    void init() {
        // Fuerza TLS 1.2 (necesario para SRI)
        System.setProperty("https.protocols", "TLSv1.2");
    }

    // ======================================================
    // Helpers
    // ======================================================
    private URL classpathUrl(String resourcePath) {
        var url = Thread.currentThread().getContextClassLoader().getResource(resourcePath);
        if (url == null) throw new IllegalStateException("No se encontró recurso en classpath: " + resourcePath);
        return url;
    }

    private void applyTimeouts(Object port) {
        Map<String, Object> ctx = ((BindingProvider) port).getRequestContext();
        // Metro
        ctx.put("com.sun.xml.ws.connect.timeout", 15000);
        ctx.put("com.sun.xml.ws.request.timeout", 30000);
        // JAX-WS interno (por compatibilidad)
        ctx.put("com.sun.xml.internal.ws.connect.timeout", 15000);
        ctx.put("com.sun.xml.internal.ws.request.timeout", 30000);
        // Estándar
        ctx.put("javax.xml.ws.client.connectionTimeout", "15000");
        ctx.put("javax.xml.ws.client.receiveTimeout", "30000");
    }

    /** Aplica el endpoint real (celcer/cel), NO el WSDL local */
    private void overrideEndpoint(Object port, boolean autorizacion) {
        overrideEndpoint(port, autorizacion, ambiente);
    }

    private void overrideEndpoint(Object port, boolean autorizacion, int ambienteSolicitud) {
        int ambienteEfectivo = (ambienteSolicitud == 2) ? 2 : 1;
        String endpoint = (ambienteEfectivo == 2)
                ? (autorizacion ? epAutorizacionProd : epRecepcionProd)
                : (autorizacion ? epAutorizacionPruebas : epRecepcionPruebas);

        ((BindingProvider) port).getRequestContext()
                .put(BindingProvider.ENDPOINT_ADDRESS_PROPERTY, endpoint);

        System.out.println("[SRI] Ambiente=" + (ambienteEfectivo == 2 ? "PRODUCCIÓN" : "PRUEBAS")
                + " | Servicio=" + (autorizacion ? "AUTORIZACIÓN" : "RECEPCIÓN")
                + " | Endpoint=" + endpoint);
    }

    private String stripBom(String s) {
        if (s == null || s.isEmpty()) return s;
        return (s.charAt(0) == '\uFEFF') ? s.substring(1) : s;
    }

    /** Extrae <claveAcceso> del XML firmado */
    private String extraerClaveAcceso(String xml) throws Exception {
        String cleaned = stripBom(xml);
        var dbf = javax.xml.parsers.DocumentBuilderFactory.newInstance();
        dbf.setNamespaceAware(true);
        var doc = dbf.newDocumentBuilder()
                .parse(new org.xml.sax.InputSource(new java.io.StringReader(cleaned)));
        var list = doc.getElementsByTagName("claveAcceso");
        if (list.getLength() == 0) throw new IllegalStateException("No se encontró <claveAcceso> en el XML");
        return list.item(0).getTextContent().replaceAll("\\s+", "");
    }

    /** Setea ambiente leyendo <infoTributaria><ambiente> del XML firmado */
    public void setAmbienteFromXml(String xmlFirmado) {
        setAmbiente(inferAmbienteFromXml(xmlFirmado));
    }

    public int inferAmbienteFromXml(String xmlFirmado) {
        try {
            String xml = stripBom(xmlFirmado);
            var dbf = javax.xml.parsers.DocumentBuilderFactory.newInstance();
            dbf.setNamespaceAware(true);
            var doc = dbf.newDocumentBuilder()
                    .parse(new org.xml.sax.InputSource(new java.io.StringReader(xml)));
            var list = doc.getElementsByTagName("ambiente");
            if (list.getLength() > 0) {
                int amb = Integer.parseInt(list.item(0).getTextContent().trim());
                return amb == 2 ? 2 : 1;
            }
        } catch (Exception ignore) {
            // conserva el ambiente configurado
        }
        return getAmbiente();
    }

    // ======================================================
    // Recepción
    // ======================================================
    /** Enviar XML firmado como bytes a Recepción */
    public RespuestaSolicitud enviarFacturaFirmada(byte[] xmlBytes) throws Exception {
        return enviarFacturaFirmada(xmlBytes, ambiente);
    }

    public RespuestaSolicitud enviarFacturaFirmada(byte[] xmlBytes, int ambienteSolicitud) throws Exception {
        // Construcción del Service con WSDL local
        URL wsdlURL = classpathUrl(wsdlLocalRecepcion);
        QName qname = new QName("http://ec.gob.sri.ws.recepcion", "RecepcionComprobantesOfflineService");

        RecepcionComprobantesOfflineService service = new RecepcionComprobantesOfflineService(wsdlURL, qname);
        RecepcionComprobantesOffline port = service.getRecepcionComprobantesOfflinePort();

        applyTimeouts(port);
        overrideEndpoint(port, /* autorizacion */ false, ambienteSolicitud);

        return port.validarComprobante(xmlBytes);
    }

    /** Enviar XML firmado como String (UTF-8) */
    public RespuestaSolicitud enviarFacturaFirmadaTxt(String xmlFirmado) throws Exception {
        String xml = stripBom(xmlFirmado);
        return enviarFacturaFirmada(xml.getBytes(StandardCharsets.UTF_8));
    }

    public RespuestaSolicitud enviarFacturaFirmadaTxt(String xmlFirmado, int ambienteSolicitud) throws Exception {
        String xml = stripBom(xmlFirmado);
        return enviarFacturaFirmada(xml.getBytes(StandardCharsets.UTF_8), ambienteSolicitud);
    }

    // ======================================================
    // Autorización
    // ======================================================
    /** Consultar autorización por clave de acceso */
    public RespuestaComprobante consultarAutorizacion(String claveAcceso) throws Exception {
        return consultarAutorizacion(claveAcceso, ambiente);
    }

    public RespuestaComprobante consultarAutorizacion(String claveAcceso, int ambienteSolicitud) throws Exception {
        URL wsdlURL = classpathUrl(wsdlLocalAutorizacion);

        QName qname = new QName("http://ec.gob.sri.ws.autorizacion", "AutorizacionComprobantesOfflineService");

        AutorizacionComprobantesOfflineService service =
                new AutorizacionComprobantesOfflineService(wsdlURL, qname);
        AutorizacionComprobantesOffline port =
                service.getAutorizacionComprobantesOfflinePort();

        applyTimeouts(port);
        overrideEndpoint(port, /* autorizacion */ true, ambienteSolicitud);

        return port.autorizacionComprobante(claveAcceso.trim());
    }


    /**
     * Polling por autorización usando una función de consulta (infiere la clave desde el XML)
     */
    public RespuestaComprobante consultarAutorizacionConEspera(
            String xmlFirmado,
            Function<String, RespuestaComprobante> consultaAutorizacionFn,
            int maxIntentos,
            long sleepMillis) throws Exception {

        String clave = extraerClaveAcceso(xmlFirmado).trim();
        RespuestaComprobante ultimo = null;

        for (int i = 1; i <= maxIntentos; i++) {
            ultimo = consultaAutorizacionFn.apply(clave);

            String numStr = (ultimo.getNumeroComprobantes() == null) ? "0" : ultimo.getNumeroComprobantes().trim();
            int n;
            try { n = Integer.parseInt(numStr); } catch (NumberFormatException e) { n = 0; }

            if (n > 0 && ultimo.getAutorizaciones() != null) {
                return ultimo; // ya hay respuesta útil
            }
            Thread.sleep(sleepMillis);
        }
        return ultimo; // podría venir sin autorizaciones (pendiente)
    }

    public RespuestaComprobante consultarAutorizacionConEspera(
            String xmlFirmado,
            int ambienteSolicitud,
            int maxIntentos,
            long sleepMillis) throws Exception {

        return consultarAutorizacionConEspera(
                xmlFirmado,
                clave -> {
                    try {
                        return consultarAutorizacion(clave, ambienteSolicitud);
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                },
                maxIntentos,
                sleepMillis
        );
    }
    public AutorizacionSriResult consultar_AutorizacionConEspera(
            String xmlFirmado,
            Function<String, AutorizacionSriResult> consultaFn,
            int maxIntentos,
            long sleepMillis) throws Exception {

        String clave = extraerClaveAcceso(xmlFirmado).trim();
        AutorizacionSriResult ultimo = null;

        for (int i = 1; i <= maxIntentos; i++) {

            ultimo = consultaFn.apply(clave);

            if (ultimo != null && ultimo.isAutorizado()) {
                return ultimo; // ya autorizado
            }

            Thread.sleep(sleepMillis);
        }

        return ultimo; // retorna el último intento
    }



    public AutorizacionSriResult consultar_Autorizacion(String claveAcceso) throws Exception {
        AutorizacionSriResult result = new AutorizacionSriResult();

        // Llamas a tu método actual
        RespuestaComprobante resp = consultarAutorizacion(claveAcceso);

        if (resp == null || resp.getAutorizaciones() == null ||
                resp.getAutorizaciones().getAutorizacion().isEmpty()) {

            result.setAutorizado(false);
            result.setMensaje("Sin autorizaciones devueltas por el SRI");
            return result;
        }

        Autorizacion aut = findAutorizacionAutorizada(resp);
        if (aut == null) {
            aut = resp.getAutorizaciones().getAutorizacion().get(0);
        }

        String estado = normEstado(aut.getEstado());
        result.setMensaje(estado);

        if ("AUTORIZADO".equals(estado)) {
            result.setAutorizado(true);

            // Número de autorización
            //result.setNumeroAutorizacion(
                 //   aut.getNumeroAutorizacion() == null ? "" : aut.getNumeroAutorizacion().trim()
            //);

            // Fecha/hora de autorización
            if (aut.getFechaAutorizacion() != null) {
                result.setFechaAutorizacion(
                        aut.getFechaAutorizacion()
                                .toGregorianCalendar()
                                .toZonedDateTime()
                                .toLocalDateTime()
                );
            }

            // XML autorizado (contenido de <comprobante>)
            result.setXmlAutorizado(normalizeComprobanteXml(aut.getComprobante()));

        } else {
            result.setAutorizado(false);
            // NO AUTORIZADO, EN PROCESO, etc.
        }

        return result;
    }




    /**
     * Polling de autorización por clave de acceso con backoff exponencial y jitter.
     *
     * @param claveAcceso          clave a consultar
     * @param maxIntentos          intentos máximos (p.ej. 20)
     * @param sleepInicialMs       espera inicial entre intentos (p.ej. 2000)
     * @param sleepMaxMs           techo de espera (p.ej. 8000)
     * @param factorBackoff        multiplicador (p.ej. 1.5)
     * @param cortarEnNoAutorizado true = cortamos si llega NO AUTORIZADO
     * @return RespuestaComprobante del último intento (idealmente con estado definitivo)
     */

    //llamando desde controller
    public RespuestaComprobante consultarAutorizacionConEsperaPorClave(
            String claveAcceso,
            int maxIntentos,
            long sleepInicialMs,
            long sleepMaxMs,
            double factorBackoff,
            boolean cortarEnNoAutorizado
    ) throws Exception {

        long sleep = sleepInicialMs;
        RespuestaComprobante ultimo = null;

        for (int intento = 1; intento <= maxIntentos; intento++) {
            try {
                ultimo = consultarAutorizacion(claveAcceso);

                if (findAutorizacionAutorizada(ultimo) != null) {
                    return ultimo;
                }

                int num = parseNum(ultimo);
                if (num > 0) {
                    var a = first(ultimo);
                    String estado = (a == null) ? "" : s(a.getEstado());

                    // Corte inmediato si hay estado definitivo
                    if ("AUTORIZADO".equalsIgnoreCase(estado)) {
                        return ultimo;
                    }
                    if ("NO AUTORIZADO".equalsIgnoreCase(estado) && cortarEnNoAutorizado) {
                        return ultimo;
                    }
                    // Si hay respuesta pero aún no definitiva, seguimos esperando
                }

            } catch (Exception ex) {
                // Errores transitorios: seguimos reintentando (podrías loguear 'ex')
                // Si quieres cortar ante ciertos errores, hazlo aquí.
            }

            // Espera con backoff + jitter
            long jitter = (long)(Math.random() * 400);                // 0–400 ms
            long delay  = Math.min(sleep, sleepMaxMs) + jitter;
            Thread.sleep(delay);
            sleep = Math.max((long)(sleep * factorBackoff), sleep + 1000); // crecimiento suave
        }

        // Vuelve con lo último que se obtuvo (posiblemente PENDIENTE)
        return ultimo;
    }


    // ---- Helper: obtener la primera autorización “útil” ----
    private ec.gob.sri.ws.autorizacion.Autorizacion getFirstAuth(RespuestaComprobante rc) {
        if (rc == null || rc.getAutorizaciones() == null || rc.getAutorizaciones().getAutorizacion() == null) return null;
        return rc.getAutorizaciones().getAutorizacion().stream().findFirst().orElse(null);
    }

    // === Helpers breves ===
    private static String s(String v){ return v == null ? "" : v.trim(); }

    private int parseNum(RespuestaComprobante rc){
        if (rc == null) return 0;
        var n = rc.getNumeroComprobantes();
        try { return (n == null) ? 0 : Integer.parseInt(n.trim()); }
        catch (NumberFormatException ignore){ return 0; }
    }

    private ec.gob.sri.ws.autorizacion.Autorizacion first(RespuestaComprobante rc){
        if (rc == null || rc.getAutorizaciones() == null) return null;
        var list = rc.getAutorizaciones().getAutorizacion();
        return (list == null || list.isEmpty()) ? null : list.get(0);
    }

    private boolean isDefinitivo(String estado){
        String n = normEstado(estado);
        return "AUTORIZADO".equals(n) || "NO AUTORIZADO".equals(n);
    }

    /** Devuelve el XML autorizado (comprobante dentro de CDATA) o null. */
    public String extraerXmlAutorizado(RespuestaComprobante rc){
        Autorizacion autorizada = findAutorizacionAutorizada(rc);
        return autorizada == null ? null : normalizeComprobanteXml(autorizada.getComprobante());
    }

    private static Autorizacion findAutorizacionAutorizada(RespuestaComprobante rc) {
        if (rc == null || rc.getAutorizaciones() == null || rc.getAutorizaciones().getAutorizacion() == null) {
            return null;
        }
        for (Autorizacion aut : rc.getAutorizaciones().getAutorizacion()) {
            if (aut != null && "AUTORIZADO".equals(normEstado(aut.getEstado()))) {
                String xml = normalizeComprobanteXml(aut.getComprobante());
                if (xml != null && !xml.isBlank()) {
                    return aut;
                }
            }
        }
        return null;
    }

    private static String normalizeComprobanteXml(String xml) {
        String comp = s(xml);
        if (comp.startsWith("<![CDATA[")) {
            comp = comp.substring(9);
        }
        if (comp.endsWith("]]>")) {
            comp = comp.substring(0, comp.length() - 3);
        }
        if (!comp.isEmpty() && comp.charAt(0) == '\uFEFF') {
            comp = comp.substring(1);
        }
        return comp.trim();
    }

    private static String normEstado(String estado) {
        String normalized = s(estado);
        normalized = Normalizer.normalize(normalized, Normalizer.Form.NFD).replaceAll("\\p{M}+", "");
        return normalized.trim().replaceAll("\\s+", " ").toUpperCase(Locale.ROOT);
    }

}
