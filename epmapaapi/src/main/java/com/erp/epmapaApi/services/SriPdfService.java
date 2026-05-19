package com.erp.epmapaApi.services;

import com.erp.epmapaApi.repositories.sri.Tabla15R;
import lombok.RequiredArgsConstructor;
import net.sf.jasperreports.engine.JRDataSource;
import net.sf.jasperreports.engine.JasperCompileManager;
import net.sf.jasperreports.engine.JasperExportManager;
import net.sf.jasperreports.engine.JasperFillManager;
import net.sf.jasperreports.engine.JasperPrint;
import net.sf.jasperreports.engine.JasperReport;
import net.sf.jasperreports.engine.data.JRBeanCollectionDataSource;
import org.springframework.stereotype.Service;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.StringReader;
import java.io.StringWriter;
import java.math.BigDecimal;
import java.text.Normalizer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

@Service
@RequiredArgsConstructor
public class SriPdfService {

    private final Tabla15R tabla15R;

    public ByteArrayOutputStream generarFacturaPDF(String xmlAutorizado, String referenciaDb, String guiaRemisionDb) {
        if (xmlAutorizado == null || xmlAutorizado.isBlank()) {
            throw new IllegalArgumentException("XML autorizado vacío o nulo para generar el PDF");
        }
        try {
            String basePath = "/reports/";

            Function<String, BigDecimal> safeBigDecimal = value -> {
                try {
                    return new BigDecimal(value == null || value.isEmpty() ? "0" : value);
                } catch (Exception e) {
                    return BigDecimal.ZERO;
                }
            };

            String cleanedXml = xmlAutorizado.trim();
            cleanedXml = cleanedXml.replace("\uFEFF", "").replace("\uFFFE", "").replace("\u00BB", "");
            if (!cleanedXml.startsWith("<?xml")) {
                cleanedXml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" + cleanedXml;
            }

            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setNamespaceAware(true);
            DocumentBuilder builder = factory.newDocumentBuilder();
            InputSource inputSource = new InputSource(new StringReader(cleanedXml));
            Document originalDoc = builder.parse(inputSource);

            String numeroAutorizacion = getNodeText(originalDoc, "numeroAutorizacion");
            String fechaAutorizacion = getNodeText(originalDoc, "fechaAutorizacion");

            Document document;
            NodeList comprobanteNodes = originalDoc.getElementsByTagName("comprobante");
            if (comprobanteNodes.getLength() > 0) {
                Node comprobante = comprobanteNodes.item(0);
                String innerXml = getInnerXml(comprobante);
                if (innerXml.isBlank()) {
                    document = originalDoc;
                } else {
                    document = builder.parse(new InputSource(new StringReader(innerXml)));
                }
            } else {
                document = originalDoc;
            }

            String razonSocial = getNodeText(document, "razonSocial");
            String ruc = getNodeText(document, "ruc");
            String direccionMatriz = getNodeText(document, "dirMatriz");
            String nombreComercial = getNodeText(document, "nombreComercial");
            String estab = getNodeText(document, "estab");
            String ptoEmi = getNodeText(document, "ptoEmi");
            String secuencial = getNodeText(document, "secuencial");
            String ambiente = getNodeText(document, "ambiente");
            String claveAcceso = getNodeText(document, "claveAcceso");

            String fechaEmision = getNodeText(document, "fechaEmision");
            String totalSinImpuestos = getNodeText(document, "totalSinImpuestos");
            String totalDescuento = getNodeText(document, "totalDescuento");
            String importeTotal = getNodeText(document, "importeTotal");
            String obligadoContabilidad = getNodeText(document, "obligadoContabilidad");
            String razonSocialComprador = getNodeText(document, "razonSocialComprador");
            String identificacionComprador = getNodeText(document, "identificacionComprador");
            String direccionComprador = getNodeText(document, "direccionComprador");
            String propina = getNodeText(document, "propina");

            String codigoFormaPago = getNodeText(document, "formaPago");
            String formaPago = tabla15R.getNombre(codigoFormaPago);

            String direccionEstablecimiento = getNodeText(document, "dirEstablecimiento");
            String contribuyenteEspecial = getNodeText(document, "contribuyenteEspecial");
            String guiaRemision = getNodeText(document, "guiaRemision");
            if (guiaRemision == null || guiaRemision.isBlank()) {
                guiaRemision = guiaRemisionDb;
            }
            String telefono = getCampoAdicional(document, "Teléfono");
            String nroFactura = estab + "-" + ptoEmi + "-" + secuencial;

            NodeList items = document.getElementsByTagName("detalle");
            List<Map<String, String>> itemsList = new ArrayList<>();
            for (int i = 0; i < items.getLength(); i++) {
                Element itemElement = (Element) items.item(i);
                Map<String, String> item = new HashMap<>();
                item.put("Codigo", getChildText(itemElement, "codigoPrincipal"));
                item.put("Descripcion", getChildText(itemElement, "descripcion"));
                item.put("Cantidad", getChildText(itemElement, "cantidad"));
                item.put("PrecioUnitario", getChildText(itemElement, "precioUnitario"));
                item.put("PrecioTotalSinImpuesto", getChildText(itemElement, "precioTotalSinImpuesto"));
                itemsList.add(item);
            }

            NodeList impuestos = document.getElementsByTagName("totalImpuesto");
            BigDecimal subtotalIVA15 = BigDecimal.ZERO;
            BigDecimal subtotalIVA12 = BigDecimal.ZERO;
            BigDecimal subtotalIVA0 = BigDecimal.ZERO;
            BigDecimal subtotalNoObjetoIVA = BigDecimal.ZERO;
            BigDecimal subtotalExentoIVA = BigDecimal.ZERO;
            BigDecimal totalIVA15 = BigDecimal.ZERO;
            BigDecimal totalIVA12 = BigDecimal.ZERO;
            BigDecimal totalICE = BigDecimal.ZERO;
            BigDecimal totalIRBPNR = BigDecimal.ZERO;

            for (int i = 0; i < impuestos.getLength(); i++) {
                Element impuesto = (Element) impuestos.item(i);
                String codigo = getChildText(impuesto, "codigo");
                String codigoPorcentaje = getChildText(impuesto, "codigoPorcentaje");
                BigDecimal baseImponible = safeBigDecimal.apply(getChildText(impuesto, "baseImponible"));
                BigDecimal valor = safeBigDecimal.apply(getChildText(impuesto, "valor"));

                if ("2".equals(codigoPorcentaje)) {
                    subtotalIVA12 = subtotalIVA12.add(baseImponible);
                    totalIVA12 = totalIVA12.add(valor);
                } else if ("3".equals(codigoPorcentaje) || "4".equals(codigoPorcentaje)) {
                    subtotalIVA15 = subtotalIVA15.add(baseImponible);
                    totalIVA15 = totalIVA15.add(valor);
                } else if ("0".equals(codigoPorcentaje)) {
                    subtotalIVA0 = subtotalIVA0.add(baseImponible);
                } else if ("6".equals(codigoPorcentaje)) {
                    subtotalNoObjetoIVA = subtotalNoObjetoIVA.add(baseImponible);
                } else if ("7".equals(codigoPorcentaje)) {
                    subtotalExentoIVA = subtotalExentoIVA.add(baseImponible);
                }

                if ("3".equals(codigo)) {
                    totalICE = totalICE.add(valor);
                } else if ("5".equals(codigo)) {
                    totalIRBPNR = totalIRBPNR.add(valor);
                }
            }

            NodeList infoAdicional = document.getElementsByTagName("campoAdicional");
            Map<String, Object> parameters = new HashMap<>();

            for (int i = 0; i < infoAdicional.getLength(); i++) {
                Element campo = (Element) infoAdicional.item(i);
                String nombreRaw = campo.getAttribute("nombre");
                String valorRaw = campo.getTextContent();
                String nombreFixed = fixEncodingIfNeeded(nombreRaw);
                String valorFixed = fixEncodingIfNeeded(valorRaw);
                String nombreNormal = sinAcentos(nombreFixed).toLowerCase();

                if (nombreNormal.contains("e-mail") || nombreNormal.contains("email")) {
                    parameters.put("Email", valorFixed);
                } else if (nombreNormal.equals("concepto")) {
                    parameters.put("Concepto", valorFixed);
                } else if (nombreNormal.equals("recaudador")) {
                    parameters.put("Recaudador", valorFixed);
                } else if (nombreNormal.equals("cuenta") || nombreNormal.equals("referencia")) {
                    parameters.put("Referencia", valorFixed);
                }
            }
            if (!parameters.containsKey("Referencia") && referenciaDb != null) {
                parameters.put("Referencia", referenciaDb);
            }

            String numeroAutorizacionSeguro =
                    (numeroAutorizacion != null && !numeroAutorizacion.trim().isEmpty())
                            ? numeroAutorizacion.trim()
                            : (claveAcceso != null ? claveAcceso.trim() : "00000000000000000000");

            parameters.put("RazonSocial", razonSocial);
            parameters.put("Ruc", ruc);
            parameters.put("NumeroAutorizacion", numeroAutorizacionSeguro);
            parameters.put("ClaveAcceso", claveAcceso);
            parameters.put("FechaAutorizacion", fechaAutorizacion);
            parameters.put("FechaEmision", fechaEmision);
            parameters.put("TotalSinImpuestos", totalSinImpuestos);
            parameters.put("DireccionMatriz", direccionMatriz);
            parameters.put("DireccionEstablecimiento", direccionEstablecimiento);
            parameters.put("Telefono", telefono);
            parameters.put("NombreComercial", nombreComercial);
            parameters.put("ObligadoContabilidad", obligadoContabilidad);
            parameters.put("ContribuyenteEspecial", contribuyenteEspecial);
            parameters.put("NroFactura", nroFactura);
            parameters.put("Ambiente", ambiente);
            parameters.put("AgenteRetencion", "00000001");
            parameters.put("RazonSocialComprador", razonSocialComprador);
            parameters.put("IdentificacionComprador", identificacionComprador);
            parameters.put("DireccionComprador", direccionComprador);
            parameters.put("GuiaRemision", guiaRemision);
            parameters.put("FormaPago", formaPago);
            parameters.put("TotalDescuento", safeBigDecimal.apply(totalDescuento));
            parameters.put("Propina", safeBigDecimal.apply(propina));
            parameters.put("ImporteTotal", safeBigDecimal.apply(importeTotal));

            parameters.put("SubTotalIVA15", subtotalIVA15);
            parameters.put("SubTotalIVA12", subtotalIVA12);
            parameters.put("SubTotalIVA0", subtotalIVA0);
            parameters.put("SubTotalNoObjetoIVA", subtotalNoObjetoIVA);
            parameters.put("SubTotalExentoIVA", subtotalExentoIVA);
            parameters.put("TotalIVA15", totalIVA15);
            parameters.put("TotalIVA12", totalIVA12);
            parameters.put("TotalICE", totalICE);
            parameters.put("TotalIRBPNR", totalIRBPNR);

            InputStream logoStream = getClass().getResourceAsStream("/reports/LOGO-H.png");
            parameters.put("LogoImage", logoStream);

            String path = basePath + "factura_template.jrxml";
            InputStream reportStream = getClass().getResourceAsStream(path);
            if (reportStream == null) {
                throw new RuntimeException("Plantilla factura_template.jrxml no encontrada");
            }

            JasperReport jasperReport = JasperCompileManager.compileReport(reportStream);
            JRDataSource itemsDataSource = new JRBeanCollectionDataSource(itemsList);
            JasperPrint jasperPrint = JasperFillManager.fillReport(jasperReport, parameters, itemsDataSource);

            ByteArrayOutputStream pdfStream = new ByteArrayOutputStream();
            JasperExportManager.exportReportToPdfStream(jasperPrint, pdfStream);
            return pdfStream;

        } catch (Exception e) {
            throw new RuntimeException("Error al generar PDF", e);
        }
    }

    private String getCampoAdicional(Document document, String nombreCampoBuscado) {
        if (nombreCampoBuscado == null) return "";
        String buscadoNormal = sinAcentos(nombreCampoBuscado).toLowerCase();
        NodeList infoAdicional = document.getElementsByTagName("campoAdicional");
        for (int i = 0; i < infoAdicional.getLength(); i++) {
            Element campo = (Element) infoAdicional.item(i);
            String nombreRaw = campo.getAttribute("nombre");
            String nombreFixed = fixEncodingIfNeeded(nombreRaw);
            if (nombreFixed.equals("e-mail")) nombreFixed = "Email";
            String actualNormal = sinAcentos(nombreFixed).toLowerCase();
            if (actualNormal.equals(buscadoNormal)) {
                String valor = campo.getTextContent();
                return valor == null ? "" : fixEncodingIfNeeded(valor.trim());
            }
        }
        return "";
    }

    private String fixEncodingIfNeeded(String text) {
        if (text == null) return null;
        String trimmed = text.trim();
        if (trimmed.isEmpty()) return trimmed;
        if (trimmed.contains("Ã") || trimmed.contains("Â")) {
            byte[] bytes = trimmed.getBytes(StandardCharsets.ISO_8859_1);
            return new String(bytes, StandardCharsets.UTF_8).trim();
        }
        return trimmed;
    }

    private String sinAcentos(String s) {
        if (s == null) return "";
        String norm = Normalizer.normalize(s, Normalizer.Form.NFD);
        return norm.replaceAll("\\p{M}", "");
    }

    private String getNodeText(Document doc, String tagName) {
        NodeList nodes = doc.getElementsByTagName(tagName);
        return nodes.getLength() > 0 ? nodes.item(0).getTextContent() : "";
    }

    private String getChildText(Element element, String tagName) {
        NodeList nodes = element.getElementsByTagName(tagName);
        return nodes.getLength() > 0 ? nodes.item(0).getTextContent() : "";
    }

    private String getInnerXml(Node parent) throws Exception {
        TransformerFactory tf = TransformerFactory.newInstance();
        Transformer transformer = tf.newTransformer();
        transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
        transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");

        NodeList children = parent.getChildNodes();
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < children.getLength(); i++) {
            Node child = children.item(i);
            short type = child.getNodeType();
            if (type == Node.ELEMENT_NODE || type == Node.CDATA_SECTION_NODE
                    || type == Node.TEXT_NODE || type == Node.PROCESSING_INSTRUCTION_NODE) {
                StringWriter sw = new StringWriter();
                transformer.transform(new DOMSource(child), new StreamResult(sw));
                sb.append(sw.toString());
            }
        }
        return sb.toString().trim();
    }
}
