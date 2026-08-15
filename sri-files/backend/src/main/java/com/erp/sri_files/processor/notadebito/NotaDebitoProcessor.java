package com.erp.sri_files.processor.notadebito;

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
public class NotaDebitoProcessor implements DocumentoProcessor {

    private static final DateTimeFormatter SRI_DATE_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private final ObjectMapper objectMapper;

    public NotaDebitoProcessor(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public TipoDocumento soporta() {
        return TipoDocumento.NOTA_DEBITO;
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
            validateMotivos(root.path("motivos"), root.at("/documento/motivo").asText());
        } catch (DocumentoRecepcionException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new DocumentoRecepcionException("No se pudo validar la estructura base de la nota de debito");
        }
    }

    private void require(JsonNode root, String pointer, String field) {
        JsonNode node = root.at(pointer);
        if (node.isMissingNode() || node.isNull() || node.asText().isBlank()) {
            throw new DocumentoRecepcionException("El campo " + field + " es obligatorio para la nota de debito");
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

    private void validateMotivos(JsonNode motivosNode, String motivoFallback) {
        if (motivosNode.isArray() && !motivosNode.isEmpty()) {
            for (int i = 0; i < motivosNode.size(); i++) {
                JsonNode motivoNode = motivosNode.get(i);
                requireNode(motivoNode, "razon", "motivos[" + i + "].razon");
                requireNode(motivoNode, "valor", "motivos[" + i + "].valor");
                validateValor(motivoNode.path("valor").asText(), i);
            }
            return;
        }

        if (motivoFallback == null || motivoFallback.isBlank()) {
            throw new DocumentoRecepcionException("La nota de debito debe incluir al menos un motivo");
        }
    }

    private void requireNode(JsonNode parent, String fieldName, String field) {
        JsonNode node = parent.path(fieldName);
        if (node.isMissingNode() || node.isNull() || node.asText().isBlank()) {
            throw new DocumentoRecepcionException("El campo " + field + " es obligatorio para la nota de debito");
        }
    }

    private void validateValor(String value, int index) {
        try {
            BigDecimal valor = new BigDecimal(value.trim());
            if (valor.compareTo(BigDecimal.ZERO) <= 0) {
                throw new DocumentoRecepcionException("El campo motivos[" + index + "].valor debe ser mayor a cero");
            }
        } catch (NumberFormatException ex) {
            throw new DocumentoRecepcionException("El campo motivos[" + index + "].valor debe ser numerico");
        }
    }
}
