package com.erp.sri_files.sri.classifier;

import com.erp.sri_files.sri.model.EstadoComunicacionSri;
import com.erp.sri_files.sri.model.ResultadoSri;
import com.erp.sri_files.sri.model.ServicioSri;
import com.erp.sri_files.sri.model.TipoResultadoSri;
import ec.gob.sri.ws.autorizacion.Autorizacion;
import ec.gob.sri.ws.autorizacion.RespuestaComprobante;
import ec.gob.sri.ws.recepcion.Comprobante;
import ec.gob.sri.ws.recepcion.Mensaje;
import ec.gob.sri.ws.recepcion.RespuestaSolicitud;
import org.springframework.stereotype.Component;

import javax.net.ssl.SSLException;
import javax.xml.datatype.XMLGregorianCalendar;
import java.io.InterruptedIOException;
import java.net.ConnectException;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.text.Normalizer;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

/**
 * Centraliza la interpretación de respuestas y excepciones del SRI.
 * El código de negocio nunca debe depender de comparar textos dispersos
 * (p. ej. mensaje.contains("CLAVE ACCESO REGISTRADA")) fuera de este componente.
 */
@Component
public class SriResponseClassifier {

    public static final String CODIGO_CLAVE_REGISTRADA = "43";

    private static final Pattern HTTP_5XX =
            Pattern.compile("(?i)HTTP.*?(502|503|504|500)");
    private static final Pattern CONNECTION_RESET =
            Pattern.compile("(?i)(connection\\s+reset|connection\\s+refused)");
    private static final Pattern TIMEOUT_PATTERN =
            Pattern.compile("(?i)(timed?\\s*out|timeout)");

    /**
     * Núcleo testeable de clasificación por código/mensaje puro.
     * Invocado directamente por las pruebas unitarias.
     */
    public ResultadoSri clasificar(String codigo, String mensaje) {
        return clasificar(codigo, mensaje, 0L);
    }

    public ResultadoSri clasificar(String codigo, String mensaje, long duracionMs) {
        var base = ResultadoSri.builder()
                .codigo(norm(codigo))
                .mensaje(norm(mensaje))
                .duracionMs(duracionMs)
                .servicio(ServicioSri.RECEPCION.name())
                .estadoComunicacion(EstadoComunicacionSri.OK);

        if (CODIGO_CLAVE_REGISTRADA.equals(norm(codigo))
                || contiene("CLAVE ACCESO REGISTRADA", mensaje)) {
            return base
                    .tipo(TipoResultadoSri.CLAVE_REGISTRADA)
                    .codigo(CODIGO_CLAVE_REGISTRADA)
                    .reintentable(false)
                    .requiereConsultaAutorizacion(true)
                    .build();
        }
        return base
                .tipo(mensaje == null || mensaje.isBlank()
                        ? TipoResultadoSri.SIN_RESPUESTA
                        : TipoResultadoSri.DEVUELTA)
                .reintentable(false)
                .requiereConsultaAutorizacion(false)
                .build();
    }

    /**
     * Clasifica la respuesta de Recepción.
     */
    public ResultadoSri clasificarRecepcion(RespuestaSolicitud rs, long duracionMs) {
        if (rs == null) {
            return sinRespuesta(ServicioSri.RECEPCION.name(), duracionMs, "Respuesta de recepción nula");
        }
        if ("RECIBIDA".equalsIgnoreCase(norm(rs.getEstado()))) {
            return ResultadoSri.builder()
                    .tipo(TipoResultadoSri.RECIBIDA)
                    .estadoComunicacion(EstadoComunicacionSri.OK)
                    .codigo(null)
                    .mensaje("RECIBIDA")
                    .reintentable(false)
                    .requiereConsultaAutorizacion(false)
                    .duracionMs(duracionMs)
                    .servicio(ServicioSri.RECEPCION.name())
                    .build();
        }
        List<Mensaje> mensajes = extraerMensajesRecepcion(rs);
        return clasificarMensajesRecepcion(mensajes, duracionMs);
    }

    /**
     * Clasifica la respuesta de Autorización.
     */
    public ResultadoSri clasificarAutorizacion(RespuestaComprobante rc, long duracionMs) {
        if (rc == null || rc.getAutorizaciones() == null
                || rc.getAutorizaciones().getAutorizacion() == null
                || rc.getAutorizaciones().getAutorizacion().isEmpty()) {
            return sinRespuesta(ServicioSri.AUTORIZACION.name(), duracionMs,
                    "Sin autorizaciones devueltas por el SRI");
        }

        List<Autorizacion> autorizaciones = rc.getAutorizaciones().getAutorizacion();
        for (Autorizacion aut : autorizaciones) {
            if (aut != null && "AUTORIZADO".equals(normEstado(aut.getEstado()))
                    && aut.getComprobante() != null && !aut.getComprobante().isBlank()) {
                return ResultadoSri.builder()
                        .tipo(TipoResultadoSri.AUTORIZADA)
                        .estadoComunicacion(EstadoComunicacionSri.OK)
                        .codigo(null)
                        .mensaje("AUTORIZADO")
                        .reintentable(false)
                        .requiereConsultaAutorizacion(false)
                        .duracionMs(duracionMs)
                        .servicio(ServicioSri.AUTORIZACION.name())
                        .numeroAutorizacion(norm(aut.getNumeroAutorizacion()))
                        .fechaAutorizacion(parseFecha(aut.getFechaAutorizacion()))
                        .xmlAutorizado(normalizarXml(aut.getComprobante()))
                        .build();
            }
        }

        Autorizacion primera = autorizaciones.get(0);
        if (primera != null && "NO AUTORIZADO".equals(normEstado(primera.getEstado()))) {
            return ResultadoSri.builder()
                    .tipo(TipoResultadoSri.NO_AUTORIZADA)
                    .estadoComunicacion(EstadoComunicacionSri.OK)
                    .codigo(null)
                    .mensaje(concatenarMensajesAutorizacion(primera))
                    .reintentable(false)
                    .requiereConsultaAutorizacion(false)
                    .duracionMs(duracionMs)
                    .servicio(ServicioSri.AUTORIZACION.name())
                    .build();
        }

        // Respuesta existe pero aún no es definitiva (RECIBIDA, EN PROCESO, AUTORIZADO sin XML, etc.)
        return sinRespuesta(ServicioSri.AUTORIZACION.name(), duracionMs,
                "Autorización pendiente o incompleta: " + norm(primera == null ? null : primera.getEstado()));
    }

    /**
     * Clasifica una excepción técnica. Nunca convierte cualquier excepción en DEVUELTA.
     */
    public ResultadoSri clasificarExcepcion(Throwable ex, String servicio, long duracionMs) {
        Throwable root = causaRaiz(ex);
        String mensaje = norm(root != null ? root.getMessage()
                : (ex != null ? ex.getMessage() : "Excepción desconocida"));
        String nombre = root != null ? root.getClass().getSimpleName()
                : (ex != null ? ex.getClass().getSimpleName() : "?");

        EstadoComunicacionSri estado;
        boolean transitorio;

        if (root instanceof SocketTimeoutException
                || root instanceof java.util.concurrent.TimeoutException
                || root instanceof InterruptedIOException) {
            estado = EstadoComunicacionSri.TIMEOUT;
            transitorio = true;
        } else if (root instanceof SocketException || root instanceof ConnectException) {
            estado = EstadoComunicacionSri.CONNECTION_RESET;
            transitorio = true;
        } else if (root instanceof UnknownHostException) {
            estado = EstadoComunicacionSri.SRI_NO_DISPONIBLE;
            transitorio = true;
        } else if (root instanceof SSLException) {
            estado = EstadoComunicacionSri.SRI_NO_DISPONIBLE;
            transitorio = true;
        } else if (HTTP_5XX.matcher(mensaje).find() || HTTP_5XX.matcher(nombre).find()) {
            estado = EstadoComunicacionSri.HTTP_ERROR;
            transitorio = true;
        } else if (CONNECTION_RESET.matcher(mensaje).find()) {
            estado = EstadoComunicacionSri.CONNECTION_RESET;
            transitorio = true;
        } else if (TIMEOUT_PATTERN.matcher(mensaje).find()) {
            estado = EstadoComunicacionSri.TIMEOUT;
            transitorio = true;
        } else if (ex instanceof jakarta.xml.ws.WebServiceException) {
            estado = EstadoComunicacionSri.SOAP_ERROR;
            transitorio = true;
        } else {
            estado = EstadoComunicacionSri.RESULTADO_INCIERTO;
            transitorio = false;
        }

        return ResultadoSri.builder()
                .tipo(transitorio
                        ? TipoResultadoSri.ERROR_TRANSITORIO
                        : TipoResultadoSri.ERROR_NO_RECUPERABLE)
                .estadoComunicacion(estado)
                .codigo(null)
                .mensaje(mensaje)
                .reintentable(transitorio)
                .requiereConsultaAutorizacion(transitorio)
                .excepcion(nombre + ": " + mensaje)
                .duracionMs(duracionMs)
                .servicio(servicio == null ? ServicioSri.RECEPCION.name() : servicio)
                .build();
    }

    // ============================================================
    // Helpers
    // ============================================================

    private ResultadoSri clasificarMensajesRecepcion(List<Mensaje> mensajes, long duracionMs) {
        var base = ResultadoSri.builder()
                .duracionMs(duracionMs)
                .servicio(ServicioSri.RECEPCION.name())
                .estadoComunicacion(EstadoComunicacionSri.OK)
                .reintentable(false);

        if (mensajes == null || mensajes.isEmpty()) {
            return base
                    .tipo(TipoResultadoSri.DEVUELTA)
                    .codigo(null)
                    .mensaje("DEVUELTA sin mensajes")
                    .requiereConsultaAutorizacion(false)
                    .build();
        }

        for (Mensaje m : mensajes) {
            if (CODIGO_CLAVE_REGISTRADA.equals(norm(m.getIdentificador()))
                    || contiene("CLAVE ACCESO REGISTRADA", m.getMensaje())) {
                return base
                        .tipo(TipoResultadoSri.CLAVE_REGISTRADA)
                        .codigo(CODIGO_CLAVE_REGISTRADA)
                        .mensaje(norm(m.getMensaje()))
                        .informacionAdicional(norm(m.getInformacionAdicional()))
                        .reintentable(false)
                        .requiereConsultaAutorizacion(true)
                        .build();
            }
        }

        Mensaje primero = mensajes.get(0);
        return base
                .tipo(TipoResultadoSri.DEVUELTA)
                .codigo(norm(primero.getIdentificador()))
                .mensaje(concatenarMensajesRecepcion(mensajes))
                .requiereConsultaAutorizacion(false)
                .build();
    }

    private List<Mensaje> extraerMensajesRecepcion(RespuestaSolicitud rs) {
        List<Mensaje> out = new ArrayList<>();
        if (rs.getComprobantes() != null && rs.getComprobantes().getComprobante() != null) {
            for (Comprobante comp : rs.getComprobantes().getComprobante()) {
                if (comp == null || comp.getMensajes() == null || comp.getMensajes().getMensaje() == null) {
                    continue;
                }
                out.addAll(comp.getMensajes().getMensaje());
            }
        }
        return out;
    }

    private ResultadoSri sinRespuesta(String servicio, long duracionMs, String mensaje) {
        return ResultadoSri.builder()
                .tipo(TipoResultadoSri.SIN_RESPUESTA)
                .estadoComunicacion(EstadoComunicacionSri.RESULTADO_INCIERTO)
                .codigo(null)
                .mensaje(mensaje)
                .reintentable(true)
                .requiereConsultaAutorizacion(true)
                .duracionMs(duracionMs)
                .servicio(servicio)
                .build();
    }

    private static Throwable causaRaiz(Throwable t) {
        Throwable actual = t;
        while (actual.getCause() != null && actual.getCause() != actual) {
            actual = actual.getCause();
        }
        return actual;
    }

    private static boolean contiene(String token, String texto) {
        if (texto == null) return false;
        return texto.toUpperCase(Locale.ROOT).contains(token.toUpperCase(Locale.ROOT));
    }

    static String normalizarXml(String xml) {
        String comp = norm(xml);
        if (comp.startsWith("<![CDATA[")) comp = comp.substring(9);
        if (comp.endsWith("]]>")) comp = comp.substring(0, comp.length() - 3);
        if (comp.startsWith("\uFEFF")) comp = comp.substring(1);
        return comp.trim();
    }

    private static LocalDateTime parseFecha(XMLGregorianCalendar xgc) {
        if (xgc == null) return null;
        try {
            return xgc.toGregorianCalendar().toZonedDateTime().toLocalDateTime();
        } catch (Exception e) {
            return null;
        }
    }

    private static String concatenarMensajesAutorizacion(Autorizacion aut) {
        if (aut.getMensajes() == null || aut.getMensajes().getMensaje() == null) return "NO AUTORIZADO";
        StringBuilder sb = new StringBuilder();
        for (ec.gob.sri.ws.autorizacion.Mensaje m : aut.getMensajes().getMensaje()) {
            if (m == null) continue;
            if (sb.length() > 0) sb.append(" | ");
            sb.append("[").append(norm(m.getIdentificador())).append("] ").append(norm(m.getMensaje()));
            String ia = norm(m.getInformacionAdicional());
            if (!ia.isBlank()) sb.append(" (").append(ia).append(")");
        }
        return sb.length() == 0 ? "NO AUTORIZADO" : sb.toString();
    }

    private static String concatenarMensajesRecepcion(List<Mensaje> mensajes) {
        StringBuilder sb = new StringBuilder();
        for (Mensaje m : mensajes) {
            if (m == null) continue;
            if (sb.length() > 0) sb.append(" || ");
            sb.append("[").append(norm(m.getIdentificador())).append("] ").append(norm(m.getMensaje()));
            String ia = norm(m.getInformacionAdicional());
            if (!ia.isBlank()) sb.append(" (").append(ia).append(")");
            String tipo = norm(m.getTipo());
            if (!tipo.isBlank()) sb.append(" <").append(tipo).append(">");
        }
        return sb.length() == 0 ? "DEVUELTA" : sb.toString();
    }

    private static String normEstado(String estado) {
        String normalized = norm(estado);
        normalized = Normalizer.normalize(normalized, Normalizer.Form.NFD).replaceAll("\\p{M}+", "");
        return normalized.toUpperCase(Locale.ROOT);
    }

    private static String norm(String s) {
        return s == null ? "" : s.trim();
    }
}