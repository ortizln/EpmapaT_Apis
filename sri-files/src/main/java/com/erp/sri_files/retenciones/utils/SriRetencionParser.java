package com.erp.sri_files.retenciones.utils;

import com.erp.sri_files.retenciones.dto.ImpuestoRet;
import org.w3c.dom.*;
import javax.xml.parsers.*;
import javax.xml.xpath.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.math.BigDecimal;

public class SriRetencionParser {

    private static Document parseXml(String xml) throws Exception {
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        dbf.setNamespaceAware(true);

        // Seguridad básica (evitar XXE)
        dbf.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
        dbf.setFeature("http://xml.org/sax/features/external-general-entities", false);
        dbf.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
        dbf.setXIncludeAware(false);
        dbf.setExpandEntityReferences(false);

        DocumentBuilder db = dbf.newDocumentBuilder();
        return db.parse(new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8)));
    }

    private static String xpStr(XPath xp, Node n, String expr) throws Exception {
        String v = (String) xp.evaluate(expr, n, XPathConstants.STRING);
        return v == null ? "" : v.trim();
    }

    private static NodeList xpNodes(XPath xp, Node n, String expr) throws Exception {
        return (NodeList) xp.evaluate(expr, n, XPathConstants.NODESET);
    }

    public static class RetencionData {
        public Map<String, Object> params = new HashMap<>();
        public List<ImpuestoRet> impuestos = new ArrayList<>();
    }

    public static RetencionData parseAutorizacion(String xmlAutorizacion) throws Exception {
        XPath xp = XPathFactory.newInstance().newXPath();
        Document docAut = parseXml(xmlAutorizacion);

        RetencionData out = new RetencionData();

        // AUTORIZACION (outer)
        out.params.put("estado", xpStr(xp, docAut, "/*[local-name()='autorizacion']/*[local-name()='estado']"));
        out.params.put("numeroAutorizacion", xpStr(xp, docAut, "/*[local-name()='autorizacion']/*[local-name()='numeroAutorizacion']"));
        out.params.put("fechaAutorizacion", xpStr(xp, docAut, "/*[local-name()='autorizacion']/*[local-name()='fechaAutorizacion']"));
        out.params.put("ambienteAutorizacion", xpStr(xp, docAut, "/*[local-name()='autorizacion']/*[local-name()='ambiente']"));

        // Extraer XML del comprobante (CDATA)
        String inner = xpStr(xp, docAut, "/*[local-name()='autorizacion']/*[local-name()='comprobante']");
        inner = inner.trim();

        // Parse inner comprobanteRetencion
        Document docComp = parseXml(inner);

        // infoTributaria
        out.params.put("razonSocial", xpStr(xp, docComp, "/*[local-name()='comprobanteRetencion']/*[local-name()='infoTributaria']/*[local-name()='razonSocial']"));
        out.params.put("nombreComercial", xpStr(xp, docComp, "/*[local-name()='comprobanteRetencion']/*[local-name()='infoTributaria']/*[local-name()='nombreComercial']"));
        out.params.put("ruc", xpStr(xp, docComp, "/*[local-name()='comprobanteRetencion']/*[local-name()='infoTributaria']/*[local-name()='ruc']"));
        out.params.put("claveAcceso", xpStr(xp, docComp, "/*[local-name()='comprobanteRetencion']/*[local-name()='infoTributaria']/*[local-name()='claveAcceso']"));
        out.params.put("estab", xpStr(xp, docComp, "/*[local-name()='comprobanteRetencion']/*[local-name()='infoTributaria']/*[local-name()='estab']"));
        out.params.put("ptoEmi", xpStr(xp, docComp, "/*[local-name()='comprobanteRetencion']/*[local-name()='infoTributaria']/*[local-name()='ptoEmi']"));
        out.params.put("secuencial", xpStr(xp, docComp, "/*[local-name()='comprobanteRetencion']/*[local-name()='infoTributaria']/*[local-name()='secuencial']"));
        out.params.put("dirMatriz", xpStr(xp, docComp, "/*[local-name()='comprobanteRetencion']/*[local-name()='infoTributaria']/*[local-name()='dirMatriz']"));

        // infoCompRetencion
        out.params.put("fechaEmision", xpStr(xp, docComp, "/*[local-name()='comprobanteRetencion']/*[local-name()='infoCompRetencion']/*[local-name()='fechaEmision']"));
        out.params.put("obligadoContabilidad", xpStr(xp, docComp, "/*[local-name()='comprobanteRetencion']/*[local-name()='infoCompRetencion']/*[local-name()='obligadoContabilidad']"));
        out.params.put("tipoIdentificacionSujetoRetenido", xpStr(xp, docComp, "/*[local-name()='comprobanteRetencion']/*[local-name()='infoCompRetencion']/*[local-name()='tipoIdentificacionSujetoRetenido']"));
        out.params.put("razonSocialSujetoRetenido", xpStr(xp, docComp, "/*[local-name()='comprobanteRetencion']/*[local-name()='infoCompRetencion']/*[local-name()='razonSocialSujetoRetenido']"));
        out.params.put("identificacionSujetoRetenido", xpStr(xp, docComp, "/*[local-name()='comprobanteRetencion']/*[local-name()='infoCompRetencion']/*[local-name()='identificacionSujetoRetenido']"));
        out.params.put("periodoFiscal", xpStr(xp, docComp, "/*[local-name()='comprobanteRetencion']/*[local-name()='infoCompRetencion']/*[local-name()='periodoFiscal']"));

        // infoAdicional (campoAdicional con atributo nombre)
        out.params.put("direccionSujetoRetenido",
                xpStr(xp, docComp, "/*[local-name()='comprobanteRetencion']/*[local-name()='infoAdicional']/*[local-name()='campoAdicional'][@nombre='Dirección']"));
        out.params.put("telefonoSujetoRetenido",
                xpStr(xp, docComp, "/*[local-name()='comprobanteRetencion']/*[local-name()='infoAdicional']/*[local-name()='campoAdicional'][@nombre='Teléfono']"));
        out.params.put("emailSujetoRetenido",
                xpStr(xp, docComp, "/*[local-name()='comprobanteRetencion']/*[local-name()='infoAdicional']/*[local-name()='campoAdicional'][@nombre='Email']"));

        // impuestos
        NodeList nodes = xpNodes(xp, docComp, "/*[local-name()='comprobanteRetencion']/*[local-name()='impuestos']/*[local-name()='impuesto']");
        for (int i = 0; i < nodes.getLength(); i++) {
            Node imp = nodes.item(i);
            ImpuestoRet it = new ImpuestoRet();
            it.codigo = xpStr(xp, imp, "*[local-name()='codigo']");
            it.codigoRetencion = xpStr(xp, imp, "*[local-name()='codigoRetencion']");
            it.baseImponible = new BigDecimal(xpStr(xp, imp, "*[local-name()='baseImponible']").isEmpty() ? "0" : xpStr(xp, imp, "*[local-name()='baseImponible']"));
            it.porcentajeRetener = new BigDecimal(xpStr(xp, imp, "*[local-name()='porcentajeRetener']").isEmpty() ? "0" : xpStr(xp, imp, "*[local-name()='porcentajeRetener']"));
            it.valorRetenido = new BigDecimal(xpStr(xp, imp, "*[local-name()='valorRetenido']").isEmpty() ? "0" : xpStr(xp, imp, "*[local-name()='valorRetenido']"));
            it.codDocSustento = xpStr(xp, imp, "*[local-name()='codDocSustento']");
            it.numDocSustento = xpStr(xp, imp, "*[local-name()='numDocSustento']");
            it.fechaEmisionDocSustento = xpStr(xp, imp, "*[local-name()='fechaEmisionDocSustento']");
            out.impuestos.add(it);
        }

        return out;
    }
}
