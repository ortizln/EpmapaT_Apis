package com.erp.sri_files.services;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.InputStream;
import java.io.StringReader;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import com.erp.sri_files.repositories.Tabla15R;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import com.itextpdf.html2pdf.HtmlConverter;

import net.sf.jasperreports.engine.JRDataSource;
import net.sf.jasperreports.engine.JasperCompileManager;
import net.sf.jasperreports.engine.JasperExportManager;
import net.sf.jasperreports.engine.JasperFillManager;
import net.sf.jasperreports.engine.JasperPrint;
import net.sf.jasperreports.engine.JasperReport;
import net.sf.jasperreports.engine.data.JRBeanCollectionDataSource;

@Service
public class XmlToPdfService {
    @Autowired
    FacturaXmlGeneratorService facturaXmlGeneratorService;
    @Autowired
    private Tabla15R tabla15r;
    @Value("${app.reports.path}")
    private String reportsPath;

    public void generarPdfDesdeXml(String xmlContent, String pdfPath) throws Exception {
        // Ruta temporal para el HTML generado
        String htmlPath = "tempFactura.html";

        // Generar HTML a partir del XML
        String htmlContent = convertirXmlAHtml(xmlContent);

        // Guardar el HTML temporalmente
        try (FileWriter writer = new FileWriter(htmlPath)) {
            writer.write(htmlContent);
        }

        // Convertir el HTML a PDF
        HtmlConverter.convertToPdf(new FileInputStream(htmlPath), new FileOutputStream(pdfPath));

        // Borrar el archivo HTML temporal
        new File(htmlPath).delete();
    }

    private String convertirXmlAHtml(String xmlContent) {
        // Aquí puedes hacer una transformación XSLT o un simple reemplazo para armar el
        // HTML.
        return "<html><body><h1>Factura Electrónica</h1><p>" + xmlContent + "</p></body></html>";
    }

    public ByteArrayOutputStream generarFacturaPDF(String xmlAutorizado) {
        try {
            if (xmlAutorizado == null || xmlAutorizado.isBlank()) {
                throw new IllegalArgumentException("XML autorizado vacío o nulo para generar el PDF");
            }
            /*String basePath = System.getProperty("os.name").toLowerCase().contains("win")
                    ? "C:/reportsEpmapat/"
                    : "/home/epmapaadmin/reportsEpmapat/";*/
            String basePath = "/reports/";

            // 1. Limpiar XML antes de parsear
            Function<String, BigDecimal> safeBigDecimal = value -> {
                try {
                    return new BigDecimal(value == null || value.isEmpty() ? "0" : value);
                } catch (Exception e) {
                    return BigDecimal.ZERO;
                }
            };

            // Parsear el XML original
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setNamespaceAware(true);
            DocumentBuilder builder = factory.newDocumentBuilder();
            InputSource inputSource = new InputSource(new StringReader(xmlAutorizado));
            inputSource.setEncoding("UTF-8");
            Document document = builder.parse(inputSource);
            String numeroAutorizacion = getNodeText(document, "numeroAutorizacion");
            // Extraer nodo <comprobante> si existe
            NodeList comprobanteNodes = document.getElementsByTagName("comprobante");
            if (comprobanteNodes.getLength() > 0) {
                String innerXml = comprobanteNodes.item(0).getTextContent();
                document = builder.parse(new InputSource(new StringReader(innerXml)));
            }

            // Validar documento
            if (document == null) {
                throw new RuntimeException("El documento XML no se ha podido transformar.");
            }

            String fechaEmision = getNodeText(document, "fechaEmision");
            // Continuar extrayendo datos como en tu implementación...
            String razonSocial = getNodeText(document, "razonSocial");
            String ruc = getNodeText(document, "ruc");

            String fechaAutorizacion = getNodeText(document, "fechaAutorizacion");
            String totalSinImpuestos = getNodeText(document, "totalSinImpuestos");
            String importeTotal = getNodeText(document, "importeTotal");
            String direccionMatriz = getNodeText(document, "dirMatriz");
            String direccionEstablecimiento = getNodeText(document, "dirEstablecimiento");
            String telefono = getNodeText(document, "telefono");
            String nombreComercial = getNodeText(document, "nombreComercial");
            String obligadoContabilidad = getNodeText(document, "obligadoContabilidad");
            String contribuyenteEspecial = getNodeText(document, "contribuyenteEspecial");
            String nroFactura = getNodeText(document, "estab") + "-" + getNodeText(document, "ptoEmi") + "-"
                    + getNodeText(document, "secuencial");
            String ambiente = getNodeText(document, "ambiente");
            String razonSocialComprador = getNodeText(document, "razonSocialComprador");
            String identificacionComprador = getNodeText(document, "identificacionComprador");
            String direccionComprador = getNodeText(document, "direccionComprador");
            String guiaRemision = getNodeText(document, "guiaRemision");
            String formaPago = tabla15r.getNombre(getNodeText(document, "formaPago"));
            String totalDescuento = getNodeText(document, "totalDescuento");
            String propina = getNodeText(document, "propina");

            // Procesar items
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

            // Procesar impuestos
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

            // Información adicional
            NodeList infoAdicional = document.getElementsByTagName("campoAdicional");
            Map<String, Object> parameters = new HashMap<>();
            for (int i = 0; i < infoAdicional.getLength(); i++) {
                Element campo = (Element) infoAdicional.item(i);
                String nombre = campo.getAttribute("nombre");
                String valor = campo.getTextContent();
                parameters.put(nombre, valor);
            }

            // Parámetros Jasper
            parameters.put("RazonSocial", razonSocial);
            parameters.put("Ruc", ruc);
            parameters.put("NumeroAutorizacion", safeValue(numeroAutorizacion, "0000000000"));
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
            parameters.put("Referencia", "--------");
            // Compilar y llenar reporte
            String path = (basePath+ "factura_template.jrxml");
            // Compilar y llenar reporte
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
            System.err.println("Error al generar PDF: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Error al generar PDF", e);
        }
    }

    private String limpiarXml(String xml) {
        if (xml == null)
            return "";
        return xml
                .replaceAll("﻿", "") // Eliminar BOM
                .replaceAll("^\\s+", "") // Eliminar espacios/saltos antes del prolog
                .replaceAll("[^\\x09\\x0A\\x0D\\x20-\\x7E]", ""); // Eliminar caracteres invisibles
    }

    // Función de utilidad para evitar valores nulos o vacíos
    private String safeValue(String value, String defaultVal) {
        return (value == null || value.trim().isEmpty()) ? defaultVal : value;
    }

    public ByteArrayOutputStream generarFacturaPDF_v2(String xmlAutorizado) {
        try {
            if (xmlAutorizado == null || xmlAutorizado.isBlank()) {
                throw new IllegalArgumentException("XML autorizado vacío o nulo para generar el PDF");
            }
            String basePath = "/reports/";
            // Utilidad para manejo seguro de BigDecimal
            Function<String, BigDecimal> safeBigDecimal = value -> {
                try {
                    return new BigDecimal(value == null || value.isEmpty() ? "0" : value);
                } catch (Exception e) {
                    return BigDecimal.ZERO;
                }
            };

            // Parseo del XML
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            InputSource inputSource = new InputSource(new StringReader(xmlAutorizado));
            inputSource.setEncoding("UTF-8");
            Document document = builder.parse(inputSource);
            if (document == null) {
                throw new RuntimeException("El documento XML no se ha podido transformar.");
            }

            // Extraer datos generales
            String razonSocial = getNodeText(document, "razonSocial");
            String ruc = getNodeText(document, "ruc");
            String numeroAutorizacion = getNodeText(document, "numeroAutorizacion");
            String fechaAutorizacion = getNodeText(document, "fechaAutorizacion");
            String fechaEmision = getNodeText(document, "fechaEmision");
            String totalSinImpuestos = getNodeText(document, "totalSinImpuestos");
            String importeTotal = getNodeText(document, "importeTotal");
            String direccionMatriz = getNodeText(document, "dirMatriz");
            String direccionEstablecimiento = getNodeText(document, "dirEstablecimiento");
            String telefono = getNodeText(document, "telefono");
            String nombreComercial = getNodeText(document, "nombreComercial");
            String obligadoContabilidad = getNodeText(document, "obligadoContabilidad");
            String contribuyenteEspecial = getNodeText(document, "contribuyenteEspecial");
            String nroFactura = getNodeText(document, "estab") + "-" + getNodeText(document, "ptoEmi") + "-"
                    + getNodeText(document, "secuencial");
            String ambiente = getNodeText(document, "ambiente");
            String razonSocialComprador = getNodeText(document, "razonSocialComprador");
            String identificacionComprador = getNodeText(document, "identificacionComprador");
            String direccionComprador = getNodeText(document, "direccionComprador");
            String guiaRemision = getNodeText(document, "guiaRemision"); // Asumido
            String formaPago = tabla15r.getNombre(getNodeText(document, "formaPago"));
            String totalDescuento = getNodeText(document, "totalDescuento");
            String propina = getNodeText(document, "propina");

            // Procesar items
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

            // Procesar impuestos
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

            // Información adicional
            NodeList infoAdicional = document.getElementsByTagName("campoAdicional");
            Map<String, Object> parameters = new HashMap<>();
            for (int i = 0; i < infoAdicional.getLength(); i++) {
                Element campo = (Element) infoAdicional.item(i);
                String nombre = campo.getAttribute("nombre");
                String valor = campo.getTextContent();
                parameters.put(nombre, valor);
            }

            // Parámetros para Jasper
            parameters.put("RazonSocial", razonSocial);
            parameters.put("Ruc", ruc);
            parameters.put("NumeroAutorizacion", numeroAutorizacion);
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
            parameters.put("AgenteRetencion", "00000001"); // Fijo o configurable
            parameters.put("RazonSocialComprador", razonSocialComprador);
            parameters.put("IdentificacionComprador", identificacionComprador);
            parameters.put("DireccionComprador", direccionComprador);
            parameters.put("GuiaRemision", guiaRemision);
            parameters.put("FormaPago", formaPago);
            parameters.put("TotalDescuento", safeBigDecimal.apply(totalDescuento));
            parameters.put("Propina", safeBigDecimal.apply(propina));
            parameters.put("ImporteTotal", safeBigDecimal.apply(importeTotal));

            // Subtotales e impuestos
            parameters.put("SubTotalIVA15", subtotalIVA15);
            parameters.put("SubTotalIVA12", subtotalIVA12);
            parameters.put("SubTotalIVA0", subtotalIVA0);
            parameters.put("SubTotalNoObjetoIVA", subtotalNoObjetoIVA);
            parameters.put("SubTotalExentoIVA", subtotalExentoIVA);
            parameters.put("TotalIVA15", totalIVA15);
            parameters.put("TotalIVA12", totalIVA12);
            parameters.put("TotalICE", totalICE);
            parameters.put("TotalIRBPNR", totalIRBPNR);
            String path = basePath + "factura_template.jrxml";

            // Compilar y llenar reporte
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
            System.err.println("Error al generar PDF: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Error al generar PDF", e);
        }
    }

    public ByteArrayOutputStream generarFacturaPDF_v3(String xmlAutorizado) {
        try {
            if (xmlAutorizado == null || xmlAutorizado.isBlank()) {
                throw new IllegalArgumentException("XML autorizado vacío o nulo para generar el PDF");
            }
            String basePath = "/reports/";

            // Utilidad para manejo seguro de BigDecimal
            Function<String, BigDecimal> safeBigDecimal = value -> {
                try {
                    return new BigDecimal(value == null || value.isEmpty() ? "0" : value);
                } catch (Exception e) {
                    return BigDecimal.ZERO;
                }
            };

            // =========================
            // 1. Parsear XML original
            // =========================
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setNamespaceAware(true);
            DocumentBuilder builder = factory.newDocumentBuilder();
            InputSource inputSource = new InputSource(new StringReader(xmlAutorizado));
            inputSource.setEncoding("UTF-8");
            Document originalDoc = builder.parse(inputSource);

            // Posibles datos de <autorizacion> (si viene el sobre del SRI)
            String numeroAutorizacion = getNodeText(originalDoc, "numeroAutorizacion");
            String fechaAutorizacion = getNodeText(originalDoc, "fechaAutorizacion");

            // =========================
            // 2. Si viene <comprobante>, parsear SOLO la <factura>
            //    Si no, asumimos que el XML ya es <factura>
            // =========================
            Document document;
            NodeList comprobanteNodes = originalDoc.getElementsByTagName("comprobante");
            if (comprobanteNodes.getLength() > 0) {
                String innerXml = comprobanteNodes.item(0).getTextContent();
                document = builder.parse(new InputSource(new StringReader(innerXml)));
            } else {
                document = originalDoc; // es directamente la <factura>
            }

            if (document == null) {
                throw new RuntimeException("El documento XML no se ha podido transformar.");
            }

            // =========================
            // 3. Extraer datos de la FACTURA (según tu XML)
            // =========================

            // infoTributaria
            String razonSocial = getNodeText(document, "razonSocial");
            String ruc = getNodeText(document, "ruc");
            String direccionMatriz = getNodeText(document, "dirMatriz");
            String nombreComercial = getNodeText(document, "nombreComercial");
            String estab = getNodeText(document, "estab");
            String ptoEmi = getNodeText(document, "ptoEmi");
            String secuencial = getNodeText(document, "secuencial");
            String ambiente = getNodeText(document, "ambiente");
            String claveAcceso = getNodeText(document, "claveAcceso");

            // infoFactura
            String fechaEmision = getNodeText(document, "fechaEmision");
            String totalSinImpuestos = getNodeText(document, "totalSinImpuestos");
            String totalDescuento = getNodeText(document, "totalDescuento");
            String importeTotal = getNodeText(document, "importeTotal");
            String obligadoContabilidad = getNodeText(document, "obligadoContabilidad");
            String razonSocialComprador = getNodeText(document, "razonSocialComprador");
            String identificacionComprador = getNodeText(document, "identificacionComprador");
            String direccionComprador = getNodeText(document, "direccionComprador");
            String propina = getNodeText(document, "propina");

            // Forma de pago (código → nombre usando tu tabla15r)
            String codigoFormaPago = getNodeText(document, "formaPago");
            String formaPago = tabla15r.getNombre(codigoFormaPago);

            // Estos campos en tu ejemplo NO vienen, así que los dejamos opcionales
            String direccionEstablecimiento = getNodeText(document, "dirEstablecimiento"); // puede venir vacío
            String contribuyenteEspecial = getNodeText(document, "contribuyenteEspecial"); // puede venir vacío
            String guiaRemision = getNodeText(document, "guiaRemision"); // puede venir vacío

            // infoAdicional -> teléfono, etc.
            String telefono = getCampoAdicional(document, "Teléfono");

            String nroFactura = estab + "-" + ptoEmi + "-" + secuencial;

            // =========================
            // 4. Detalles de la factura
            // =========================
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

            // =========================
            // 5. Totales de impuestos
            // =========================
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

                if ("2".equals(codigoPorcentaje)) {                    // IVA 12%
                    subtotalIVA12 = subtotalIVA12.add(baseImponible);
                    totalIVA12 = totalIVA12.add(valor);
                } else if ("3".equals(codigoPorcentaje) || "4".equals(codigoPorcentaje)) { // IVA 14/15
                    subtotalIVA15 = subtotalIVA15.add(baseImponible);
                    totalIVA15 = totalIVA15.add(valor);
                } else if ("0".equals(codigoPorcentaje)) {             // IVA 0%
                    subtotalIVA0 = subtotalIVA0.add(baseImponible);
                } else if ("6".equals(codigoPorcentaje)) {             // No objeto
                    subtotalNoObjetoIVA = subtotalNoObjetoIVA.add(baseImponible);
                } else if ("7".equals(codigoPorcentaje)) {             // Exento
                    subtotalExentoIVA = subtotalExentoIVA.add(baseImponible);
                }

                if ("3".equals(codigo)) {          // ICE
                    totalICE = totalICE.add(valor);
                } else if ("5".equals(codigo)) {   // IRBPNR
                    totalIRBPNR = totalIRBPNR.add(valor);
                }
            }

// =========================
// 6. Información adicional → parámetros libres
// =========================
            NodeList infoAdicional = document.getElementsByTagName("campoAdicional");
            Map<String, Object> parameters = new HashMap<>();

            for (int i = 0; i < infoAdicional.getLength(); i++) {
                Element campo = (Element) infoAdicional.item(i);

                String nombreRaw = campo.getAttribute("nombre");
                String valorRaw  = campo.getTextContent();

                // Corregir encoding roto (TelÃ©fono → Teléfono)
                String nombreFixed = fixEncodingIfNeeded(nombreRaw);
                String valorFixed  = fixEncodingIfNeeded(valorRaw);

                // Normalizar para comparar (sin acentos, lower)
                String nombreNormal = sinAcentos(nombreFixed).toLowerCase();

                System.out.println("campoAdicional => [" + nombreFixed + "] = [" + valorFixed + "]");

                // Mapeos específicos a parámetros de Jasper
                if (nombreNormal.contains("e-mail") || nombreNormal.contains("email")) {
                    // JRXML: $P{Email}
                    parameters.put("Email", valorFixed);

                } else if (nombreNormal.equals("concepto")) {
                    // JRXML: $P{Concepto}
                    parameters.put("Concepto", valorFixed);

                } else if (nombreNormal.equals("recaudador")) {
                    // JRXML: $P{Recaudador}
                    parameters.put("Recaudador", valorFixed);

                } else if (nombreNormal.equals("cuenta")) {
                    // solo si quieres usarla: crea <parameter name="Cuenta" .../> en Jasper
                    parameters.put("Referencia", valorFixed);

                } else if (nombreNormal.startsWith("fechaemision")) {
                    // ya tienes $P{FechaEmision} desde infoFactura, pero si quisieras priorizar este:
                    // parameters.put("FechaEmision", valorFixed);
                } else if (nombreNormal.startsWith("telefono")) {
                    // si en el futuro agregas <parameter name="Telefono">
                    // parameters.put("Telefono", valorFixed);
                }

                // si quieres conservar TODOS los campos originales también:
                // parameters.put(nombreFixed, valorFixed);
            }

            // =========================
            // 7. Parámetros para Jasper
            // =========================

            // Si no tienes numeroAutorizacion (porque solo vino la <factura>),
            // usamos la claveAcceso para que el código de barras NUNCA quede vacío.
            String numeroAutorizacionSeguro =
                    (numeroAutorizacion != null && !numeroAutorizacion.trim().isEmpty())
                            ? numeroAutorizacion.trim()
                            : safeValue(claveAcceso, "00000000000000000000");

            parameters.put("RazonSocial", razonSocial);
            parameters.put("Ruc", ruc);
            parameters.put("NumeroAutorizacion", numeroAutorizacionSeguro);
            parameters.put("ClaveAcceso", claveAcceso); // por si tu JRXML usa este también
            parameters.put("FechaAutorizacion", fechaAutorizacion); // si vino en el sobre, se usa
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

            // Subtotales e impuestos
            parameters.put("SubTotalIVA15", subtotalIVA15);
            parameters.put("SubTotalIVA12", subtotalIVA12);
            parameters.put("SubTotalIVA0", subtotalIVA0);
            parameters.put("SubTotalNoObjetoIVA", subtotalNoObjetoIVA);
            parameters.put("SubTotalExentoIVA", subtotalExentoIVA);
            parameters.put("TotalIVA15", totalIVA15);
            parameters.put("TotalIVA12", totalIVA12);
            parameters.put("TotalICE", totalICE);
            parameters.put("TotalIRBPNR", totalIRBPNR);

            //parameters.put("Referencia", "--------");

            // =========================
            // 8. Compilar y generar PDF
            // =========================
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
            System.err.println("Error al generar PDF: " + e.getMessage());
            e.printStackTrace();
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

            // 1) Arreglar codificación si viene como "TelÃ©fono"
            String nombreFixed = fixEncodingIfNeeded(nombreRaw);
            System.out.println(nombreFixed);
            if(nombreFixed =="e-mail"){
                nombreFixed = "Email";
            }
            // 2) Comparar sin acentos y sin mayúsculas
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

        // Heurística: texto UTF-8 leído como ISO-8859-1 (TelÃ©fono, Fecha emisiÃ³n)
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

    // Helper methods for XML parsing
    private String getNodeText(Document doc, String tagName) {
        NodeList nodes = doc.getElementsByTagName(tagName);
        return nodes.getLength() > 0 ? nodes.item(0).getTextContent() : "";
    }

    private String getChildText(Element element, String tagName) {
        NodeList nodes = element.getElementsByTagName(tagName);
        return nodes.getLength() > 0 ? nodes.item(0).getTextContent() : "";
    }

}
