package com.erp.sri_files.processor.notacredito;

import com.erp.sri_files.domain.documento.DocumentoElectronico;
import com.erp.sri_files.domain.documento.TipoDocumento;
import com.erp.sri_files.exceptions.DocumentoRecepcionException;
import com.erp.sri_files.processor.DocumentoProcessor;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

@Component
public class NotaCreditoProcessor implements DocumentoProcessor {

    private static final DateTimeFormatter SRI_DATE_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private final ObjectMapper objectMapper;

    public NotaCreditoProcessor(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public TipoDocumento soporta() {
        return TipoDocumento.NOTA_CREDITO;
    }

    @Override
    public void validar(DocumentoElectronico documento) {
        try {
            JsonNode root = objectMapper.readTree(documento.getJsonOriginal());
            require(root, "/documento/motivo", "documento.motivo");
            require(root, "/documento/numeroDocumentoModificado", "documento.numeroDocumentoModificado");
            require(root, "/documento/fechaEmisionDocumentoModificado", "documento.fechaEmisionDocumentoModificado");
            require(root, "/receptor/identificacion", "receptor.identificacion");
            require(root, "/receptor/razonSocial", "receptor.razonSocial");
            validateNumeroDocumento(root.at("/documento/numeroDocumentoModificado").asText(), "documento.numeroDocumentoModificado");
            validateFecha(root.at("/documento/fechaEmisionDocumentoModificado").asText(), "documento.fechaEmisionDocumentoModificado");
            validateDetalles(root.path("detalles"));
        } catch (DocumentoRecepcionException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new DocumentoRecepcionException("No se pudo validar la estructura base de la nota de credito");
        }
    }

    private void require(JsonNode root, String pointer, String field) {
        JsonNode node = root.at(pointer);
        if (node.isMissingNode() || node.isNull() || node.asText().isBlank()) {
            throw new DocumentoRecepcionException("El campo " + field + " es obligatorio para la nota de credito");
        }
    }

    private void validateNumeroDocumento(String value, String field) {
        if (!value.trim().matches("\\d{3}-\\d{3}-\\d{9}")) {
            throw new DocumentoRecepcionException("El campo " + field + " debe tener formato 001-001-000000001");
        }
    }

    private void validateFecha(String value, String field) {
        try {
            LocalDate.parse(value.trim(), SRI_DATE_FORMAT);
        } catch (DateTimeParseException ex) {
            throw new DocumentoRecepcionException("El campo " + field + " debe tener formato dd/MM/yyyy");
        }
    }

    private void validateDetalles(JsonNode detallesNode) {
        if (!detallesNode.isArray() || detallesNode.isEmpty()) {
            throw new DocumentoRecepcionException("La nota de credito debe incluir al menos un detalle");
        }
        for (int i = 0; i < detallesNode.size(); i++) {
            JsonNode detalleNode = detallesNode.get(i);
            requireNode(detalleNode, "descripcion", "detalles[" + i + "].descripcion");
            requireNode(detalleNode, "cantidad", "detalles[" + i + "].cantidad");
            requireNode(detalleNode, "precioUnitario", "detalles[" + i + "].precioUnitario");
            validateCantidad(detalleNode.path("cantidad").asText(), i);
        }
    }

    private void requireNode(JsonNode parent, String fieldName, String field) {
        JsonNode node = parent.path(fieldName);
        if (node.isMissingNode() || node.isNull() || node.asText().isBlank()) {
            throw new DocumentoRecepcionException("El campo " + field + " es obligatorio para la nota de credito");
        }
    }

    private void validateCantidad(String value, int index) {
        try {
            BigDecimal cantidad = new BigDecimal(value.trim());
            if (cantidad.compareTo(BigDecimal.ZERO) <= 0) {
                throw new DocumentoRecepcionException("El campo detalles[" + index + "].cantidad debe ser mayor a cero");
            }
        } catch (NumberFormatException ex) {
            throw new DocumentoRecepcionException("El campo detalles[" + index + "].cantidad debe ser numerico");
        }
    }
}
