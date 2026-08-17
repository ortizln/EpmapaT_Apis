package com.erp.sri_files.service;

import com.erp.sri_files.domain.documento.TipoDocumento;
import org.springframework.stereotype.Service;
import org.w3c.dom.Document;
import org.xml.sax.InputSource;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;

@Service
public class DocumentoXmlValidationService {

    public XmlValidationResult validate(TipoDocumento tipoDocumento, String xml) {
        List<String> errors = new ArrayList<>();

        if (xml == null || xml.isBlank()) {
            errors.add("XML vacio o nulo");
            return new XmlValidationResult(false, errors);
        }

        try {
            Document document = parse(xml);
            String rootName = localName(document);

            switch (tipoDocumento) {
                case FACTURA -> validateFactura(document, rootName, errors);
                case NOTA_CREDITO -> validateNotaCredito(document, rootName, errors);
                case NOTA_DEBITO -> validateNotaDebito(document, rootName, errors);
                case GUIA_REMISION -> validateGuiaRemision(document, rootName, errors);
                case RETENCION -> validateRetencion(document, rootName, errors);
                case LIQUIDACION_COMPRA -> validateLiquidacionCompra(document, rootName, errors);
            }

            return new XmlValidationResult(errors.isEmpty(), errors);
        } catch (Exception ex) {
            errors.add("XML no parseable: " + ex.getMessage());
            return new XmlValidationResult(false, errors);
        }
    }

    private void validateFactura(Document document, String rootName, List<String> errors) throws Exception {
        requireRoot(errors, "factura", rootName);
        requireComprobanteAttributes(document, errors);
        require(errors, text(document, "codDoc"), "infoTributaria.codDoc");
        requireValue(errors, text(document, "codDoc"), "01", "infoTributaria.codDoc");
        require(errors, text(document, "ruc"), "infoTributaria.ruc");
        require(errors, text(document, "claveAcceso"), "infoTributaria.claveAcceso");
        require(errors, text(document, "fechaEmision"), "infoFactura.fechaEmision");
        require(errors, text(document, "identificacionComprador"), "infoFactura.identificacionComprador");
        require(errors, text(document, "razonSocialComprador"), "infoFactura.razonSocialComprador");
        require(errors, text(document, "importeTotal"), "infoFactura.importeTotal");
        require(errors, text(document, "descripcion"), "detalles.detalle.descripcion");
    }

    private void validateNotaCredito(Document document, String rootName, List<String> errors) throws Exception {
        requireRoot(errors, "notaCredito", rootName);
        requireComprobanteAttributes(document, errors);
        requireValue(errors, text(document, "codDoc"), "04", "infoTributaria.codDoc");
        require(errors, text(document, "claveAcceso"), "infoTributaria.claveAcceso");
        require(errors, text(document, "fechaEmision"), "infoNotaCredito.fechaEmision");
        require(errors, text(document, "numDocModificado"), "infoNotaCredito.numDocModificado");
        require(errors, text(document, "fechaEmisionDocSustento"), "infoNotaCredito.fechaEmisionDocSustento");
        require(errors, text(document, "valorModificacion"), "infoNotaCredito.valorModificacion");
        require(errors, text(document, "motivo"), "infoNotaCredito.motivo");
    }

    private void validateNotaDebito(Document document, String rootName, List<String> errors) throws Exception {
        requireRoot(errors, "notaDebito", rootName);
        requireComprobanteAttributes(document, errors);
        requireValue(errors, text(document, "codDoc"), "05", "infoTributaria.codDoc");
        require(errors, text(document, "claveAcceso"), "infoTributaria.claveAcceso");
        require(errors, text(document, "fechaEmision"), "infoNotaDebito.fechaEmision");
        require(errors, text(document, "numDocModificado"), "infoNotaDebito.numDocModificado");
        require(errors, text(document, "fechaEmisionDocSustento"), "infoNotaDebito.fechaEmisionDocSustento");
        require(errors, text(document, "valorTotal"), "infoNotaDebito.valorTotal");
        require(errors, text(document, "razon"), "motivos.motivo.razon");
    }

    private void validateGuiaRemision(Document document, String rootName, List<String> errors) throws Exception {
        requireRoot(errors, "guiaRemision", rootName);
        requireComprobanteAttributes(document, errors);
        requireValue(errors, text(document, "codDoc"), "06", "infoTributaria.codDoc");
        require(errors, text(document, "claveAcceso"), "infoTributaria.claveAcceso");
        require(errors, text(document, "dirPartida"), "infoGuiaRemision.dirPartida");
        require(errors, text(document, "fechaIniTransporte"), "infoGuiaRemision.fechaIniTransporte");
        require(errors, text(document, "fechaFinTransporte"), "infoGuiaRemision.fechaFinTransporte");
        require(errors, text(document, "placa"), "infoGuiaRemision.placa");
        require(errors, text(document, "identificacionDestinatario"), "destinatarios.destinatario.identificacionDestinatario");
        require(errors, text(document, "motivoTraslado"), "destinatarios.destinatario.motivoTraslado");
    }

    private void validateRetencion(Document document, String rootName, List<String> errors) throws Exception {
        requireRoot(errors, "comprobanteRetencion", rootName);
        requireComprobanteAttributes(document, errors);
        requireValue(errors, text(document, "codDoc"), "07", "infoTributaria.codDoc");
        require(errors, text(document, "claveAcceso"), "infoTributaria.claveAcceso");
    }

    private void validateLiquidacionCompra(Document document, String rootName, List<String> errors) {
        requireRoot(errors, "liquidacionCompra", rootName);
        requireComprobanteAttributes(document, errors);
        try {
            requireValue(errors, text(document, "codDoc"), "03", "infoTributaria.codDoc");
            require(errors, text(document, "claveAcceso"), "infoTributaria.claveAcceso");
            require(errors, text(document, "fechaEmision"), "infoLiquidacionCompra.fechaEmision");
            require(errors, text(document, "tipoIdentificacionProveedor"), "infoLiquidacionCompra.tipoIdentificacionProveedor");
            require(errors, text(document, "razonSocialProveedor"), "infoLiquidacionCompra.razonSocialProveedor");
            require(errors, text(document, "identificacionProveedor"), "infoLiquidacionCompra.identificacionProveedor");
            require(errors, text(document, "importeTotal"), "infoLiquidacionCompra.importeTotal");
            require(errors, text(document, "descripcion"), "detalles.detalle.descripcion");
        } catch (Exception ex) {
            errors.add("No se pudo validar estructura de liquidacion de compra: " + ex.getMessage());
        }
    }

    private void requireComprobanteAttributes(Document document, List<String> errors) {
        String id = document.getDocumentElement().getAttribute("id");
        if (!"comprobante".equals(id)) {
            errors.add("El atributo id debe ser \"comprobante\"");
        }

        String version = document.getDocumentElement().getAttribute("version");
        if (version == null || version.isBlank()) {
            errors.add("El atributo version es obligatorio");
        }
    }

    private void requireRoot(List<String> errors, String expected, String actual) {
        if (!expected.equals(actual)) {
            errors.add("Raiz invalida. Se esperaba <" + expected + "> y se recibio <" + actual + ">");
        }
    }

    private void require(List<String> errors, String value, String field) {
        if (value == null || value.isBlank()) {
            errors.add("Campo obligatorio ausente: " + field);
        }
    }

    private void requireValue(List<String> errors, String actual, String expected, String field) {
        if (!expected.equals(actual)) {
            errors.add("Valor invalido para " + field + ". Se esperaba " + expected + " y se recibio " + empty(actual));
        }
    }

    private Document parse(String xml) throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(true);
        factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
        factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
        factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
        factory.setXIncludeAware(false);
        factory.setExpandEntityReferences(false);
        return factory.newDocumentBuilder().parse(new InputSource(new StringReader(stripBom(xml).trim())));
    }

    private String localName(Document document) {
        String localName = document.getDocumentElement().getLocalName();
        return localName == null ? document.getDocumentElement().getNodeName() : localName;
    }

    private String text(Document document, String localName) throws Exception {
        String expression = "string(//*[local-name()='" + localName + "'][1])";
        String value = (String) XPathFactory.newInstance().newXPath().evaluate(expression, document, XPathConstants.STRING);
        return value == null ? "" : value.trim();
    }

    private String stripBom(String value) {
        return value != null && !value.isEmpty() && value.charAt(0) == '\uFEFF' ? value.substring(1) : value;
    }

    private String empty(String value) {
        return value == null || value.isBlank() ? "<vacio>" : value;
    }

    public record XmlValidationResult(boolean valid, List<String> errors) {
    }
}
