package com.erp.sri_files.utils;

import com.erp.sri_files.dto.AutorizacionInfo;
import com.erp.sri_files.dto.AutorizacionSriResult;
import ec.gob.sri.ws.autorizacion.RespuestaComprobante;
import javax.xml.datatype.XMLGregorianCalendar;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public final class SriAutorizacionAdapter {

    private SriAutorizacionAdapter() {}

    public static Optional<AutorizacionInfo> fromRespuesta(RespuestaComprobante rc) {
        if (rc == null || rc.getAutorizaciones() == null) return Optional.empty();
        List<ec.gob.sri.ws.autorizacion.Autorizacion> list = rc.getAutorizaciones().getAutorizacion();
        if (list == null || list.isEmpty()) return Optional.empty();

        var au = list.get(0);
        String estado = safe(au.getEstado()); // "AUTORIZADO" | "NO AUTORIZADO"
        boolean ok = "AUTORIZADO".equalsIgnoreCase(estado);

        String numAut = safe(au.getNumeroAutorizacion());
        LocalDateTime fecha = parseFecha(au.getFechaAutorizacion());

        byte[] xmlAut = null;
        try {
            String comprobante = au.getComprobante();
            if (comprobante != null) {
                String xml = comprobante.replace("<![CDATA[", "").replace("]]>", "");
                xmlAut = xml.getBytes(java.nio.charset.StandardCharsets.UTF_8);
            }
        } catch (Exception ignored) {}

        String mensajes = concatMensajes(au);

        return Optional.of(new AutorizacionInfo(ok, numAut, fecha, xmlAut, mensajes));
    }
    public static Optional<AutorizacionInfo> from_Resultado(AutorizacionSriResult rc) {
        if (rc == null) return Optional.empty();

        boolean ok = rc.isAutorizado();
// En el adapter, si no hay número de autorización en el DTO:
        String numAut = rc.getNumeroAutorizacion();
        if ((numAut == null || numAut.isBlank()) && rc.getXmlAutorizado() != null) {
            // ejemplo: extraer <claveAcceso>...</claveAcceso> del comprobante
            numAut = extraerClaveDesdeXml(rc.getXmlAutorizado());
        }
        LocalDateTime fecha = rc.getFechaAutorizacion();

        byte[] xmlAut = null;
        try {
            if (rc.getXmlAutorizado() != null) {
                xmlAut = rc.getXmlAutorizado().getBytes(StandardCharsets.UTF_8);
            }
        } catch (Exception ignored) {}

        String mensajes = rc.getMensaje() == null ? "" : rc.getMensaje();

        return Optional.of(new AutorizacionInfo(ok, numAut, fecha, xmlAut, mensajes));
    }
    private static String extraerClaveDesdeXml(String xml) {
        try {
            int i = xml.indexOf("<claveAcceso>");
            int j = xml.indexOf("</claveAcceso>");
            if (i >= 0 && j > i) {
                return xml.substring(i + "<claveAcceso>".length(), j).trim();
            }
        } catch (Exception ignored) {}
        return null;
    }




    private static LocalDateTime parseFecha(Object raw) {
        try {
            if (raw instanceof XMLGregorianCalendar xgc) {
                return xgc.toGregorianCalendar().toZonedDateTime().toLocalDateTime();
            } else if (raw instanceof java.util.Date d) {
                return d.toInstant().atZone(java.time.ZoneId.systemDefault()).toLocalDateTime();
            } else if (raw instanceof String s && !s.isBlank()) {
                String t = s.replace("Z", "");
                if (t.length() >= 19) t = t.substring(0, 19);
                return LocalDateTime.parse(t);
            }
        } catch (Exception ignored) {}
        return null;
    }

    private static String concatMensajes(ec.gob.sri.ws.autorizacion.Autorizacion au) {
        if (au.getMensajes() == null || au.getMensajes().getMensaje() == null) return null;
        StringBuilder sb = new StringBuilder();
        for (ec.gob.sri.ws.autorizacion.Mensaje m : au.getMensajes().getMensaje()) {
            if (sb.length() > 0) sb.append(" | ");
            sb.append("[")
                    .append(safe(m.getIdentificador()))
                    .append("] ")
                    .append(safe(m.getMensaje()));
            String ia = safe(m.getInformacionAdicional());
            if (!ia.isBlank()) sb.append(" (").append(ia).append(")");
        }
        return sb.length() == 0 ? null : sb.toString();
    }

    private static String safe(String s) { return s == null ? "" : s.trim(); }
}

