package com.erp.sri_files.validation;

import org.springframework.stereotype.Service;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;
import java.io.StringReader;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Service
public class SriRetencionValidationService {

    public RetencionValidationResult validate(String xml) {
        List<String> errors = new ArrayList<>();
        List<String> warnings = new ArrayList<>();

        if (xml == null || xml.isBlank()) {
            errors.add("XML vacio o nulo");
            return new RetencionValidationResult(false, errors, warnings, null, null, null, null);
        }

        try {
            Document doc = parse(xml);
            Element root = doc.getDocumentElement();
            String rootName = root.getLocalName() == null ? root.getNodeName() : root.getLocalName();

            if (!"comprobanteRetencion".equals(rootName)) {
                errors.add("Raiz invalida. Se esperaba <comprobanteRetencion> y se recibio <" + rootName + ">");
            }

            String id = root.getAttribute("id");
            if (!"comprobante".equals(id)) {
                errors.add("El atributo id del comprobante debe ser id=\"comprobante\"");
            }

            String version = root.getAttribute("version");
            if (version == null || version.isBlank()) {
                errors.add("El atributo version es obligatorio en <comprobanteRetencion>");
            } else if (!version.matches("1\\.0\\.0|2\\.0\\.0")) {
                warnings.add("Version de retencion no reconocida por el validador local: " + version);
            }

            String claveAcceso = text(doc, "claveAcceso");
            String ambiente = text(doc, "ambiente");
            String tipoEmision = text(doc, "tipoEmision");
            String codDoc = text(doc, "codDoc");
            String ruc = text(doc, "ruc");

            require(errors, claveAcceso, "infoTributaria.claveAcceso");
            require(errors, ambiente, "infoTributaria.ambiente");
            require(errors, tipoEmision, "infoTributaria.tipoEmision");
            require(errors, codDoc, "infoTributaria.codDoc");
            require(errors, ruc, "infoTributaria.ruc");
            require(errors, text(doc, "estab"), "infoTributaria.estab");
            require(errors, text(doc, "ptoEmi"), "infoTributaria.ptoEmi");
            require(errors, text(doc, "secuencial"), "infoTributaria.secuencial");
            require(errors, text(doc, "fechaEmision"), "infoCompRetencion.fechaEmision");
            require(errors, text(doc, "periodoFiscal"), "infoCompRetencion.periodoFiscal");
            require(errors, text(doc, "tipoIdentificacionSujetoRetenido"), "infoCompRetencion.tipoIdentificacionSujetoRetenido");
            require(errors, text(doc, "identificacionSujetoRetenido"), "infoCompRetencion.identificacionSujetoRetenido");

            if (!"07".equals(codDoc)) {
                errors.add("codDoc invalido para retencion. Se esperaba 07 y se recibio " + empty(codDoc));
            }
            if (!ambiente.matches("[12]")) {
                errors.add("ambiente invalido. Valores permitidos: 1 o 2");
            }
            if (!"1".equals(tipoEmision)) {
                warnings.add("tipoEmision distinto de normal: " + empty(tipoEmision));
            }
            if (!ruc.matches("\\d{13}")) {
                errors.add("RUC invalido. Debe tener 13 digitos");
            }
            validateClaveAcceso(errors, warnings, claveAcceso, codDoc, ruc, ambiente);
            validateImpuestos(errors, doc);

            return new RetencionValidationResult(errors.isEmpty(), errors, warnings, claveAcceso, ambiente, codDoc, version);
        } catch (Exception e) {
            errors.add("XML corrupto o no parseable: " + e.getMessage());
            return new RetencionValidationResult(false, errors, warnings, null, null, null, null);
        }
    }

    private static Document parse(String xml) throws Exception {
        String clean = stripBom(xml).trim();
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        dbf.setNamespaceAware(true);
        dbf.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
        dbf.setFeature("http://xml.org/sax/features/external-general-entities", false);
        dbf.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
        dbf.setXIncludeAware(false);
        dbf.setExpandEntityReferences(false);
        return dbf.newDocumentBuilder().parse(new InputSource(new StringReader(clean)));
    }

    private static String text(Document doc, String localName) throws Exception {
        var xp = XPathFactory.newInstance().newXPath();
        String expr = "string(//*[local-name()='" + localName + "'][1])";
        String value = (String) xp.evaluate(expr, doc, XPathConstants.STRING);
        return value == null ? "" : value.trim().replaceAll("\\s+", "");
    }

    private static NodeList nodes(Document doc, String localName) throws Exception {
        var xp = XPathFactory.newInstance().newXPath();
        return (NodeList) xp.evaluate("//*[local-name()='" + localName + "']", doc, XPathConstants.NODESET);
    }

    private static void validateImpuestos(List<String> errors, Document doc) throws Exception {
        NodeList impuestosLegacy = nodes(doc, "impuesto");
        NodeList retencionesV2 = nodes(doc, "retencion");
        int totalItems = impuestosLegacy.getLength() + retencionesV2.getLength();
        if (totalItems == 0) {
            errors.add("La retencion no contiene impuestos retenidos: se esperaba <impuestos>/<impuesto> o <docsSustento>/<docSustento>/<retenciones>/<retencion>");
            return;
        }

        if (impuestosLegacy.getLength() > 0) {
            requireNodeNumber(errors, doc, "baseImponible");
            requireNodeNumber(errors, doc, "porcentajeRetener");
            requireNodeNumber(errors, doc, "valorRetenido");
            requireNode(errors, doc, "codigo");
            requireNode(errors, doc, "codigoRetencion");
        }
        if (retencionesV2.getLength() > 0) {
            requireNodeNumber(errors, doc, "baseImponible");
            requireNodeNumber(errors, doc, "porcentajeRetener");
            requireNodeNumber(errors, doc, "valorRetenido");
            requireNode(errors, doc, "codigo");
            requireNode(errors, doc, "codigoRetencion");
            requireNode(errors, doc, "codDocSustento");
            requireNode(errors, doc, "numDocSustento");
        }
    }

    private static void requireNode(List<String> errors, Document doc, String localName) throws Exception {
        if (text(doc, localName).isBlank()) {
            errors.add("Campo obligatorio ausente: " + localName);
        }
    }

    private static void requireNodeNumber(List<String> errors, Document doc, String localName) throws Exception {
        String value = text(doc, localName);
        if (value.isBlank()) {
            errors.add("Campo numerico obligatorio ausente: " + localName);
            return;
        }
        try {
            new BigDecimal(value);
        } catch (NumberFormatException e) {
            errors.add("Campo numerico invalido " + localName + "=" + value);
        }
    }

    private static void validateClaveAcceso(List<String> errors, List<String> warnings, String clave, String codDoc, String ruc, String ambiente) {
        if (clave == null || clave.isBlank()) {
            return;
        }
        if (!clave.matches("\\d{49}")) {
            errors.add("claveAcceso invalida. Debe tener 49 digitos");
            return;
        }
        if (!clave.substring(8, 10).equals(codDoc)) {
            errors.add("claveAcceso inconsistente: codDoc embebido " + clave.substring(8, 10) + " no coincide con " + codDoc);
        }
        if (!clave.substring(10, 23).equals(ruc)) {
            errors.add("claveAcceso inconsistente: RUC embebido no coincide con infoTributaria.ruc");
        }
        if (!clave.substring(23, 24).equals(ambiente)) {
            errors.add("claveAcceso inconsistente: ambiente embebido no coincide con infoTributaria.ambiente");
        }
        int expected = modulo11(clave.substring(0, 48));
        int actual = Character.digit(clave.charAt(48), 10);
        if (expected != actual) {
            warnings.add("Digito verificador de claveAcceso no coincide. Esperado " + expected + ", recibido " + actual);
        }
    }

    private static int modulo11(String first48Digits) {
        int factor = 2;
        int total = 0;
        for (int i = first48Digits.length() - 1; i >= 0; i--) {
            total += Character.digit(first48Digits.charAt(i), 10) * factor;
            factor = factor == 7 ? 2 : factor + 1;
        }
        int mod = 11 - (total % 11);
        if (mod == 11) return 0;
        if (mod == 10) return 1;
        return mod;
    }

    private static void require(List<String> errors, String value, String field) {
        if (value == null || value.isBlank()) {
            errors.add("Campo obligatorio ausente: " + field);
        }
    }

    private static String stripBom(String s) {
        return s != null && !s.isEmpty() && s.charAt(0) == '\uFEFF' ? s.substring(1) : s;
    }

    private static String empty(String value) {
        return value == null || value.isBlank() ? "<vacio>" : value;
    }

    public record RetencionValidationResult(
            boolean valid,
            List<String> errors,
            List<String> warnings,
            String claveAcceso,
            String ambiente,
            String codDoc,
            String version
    ) {
    }
}
