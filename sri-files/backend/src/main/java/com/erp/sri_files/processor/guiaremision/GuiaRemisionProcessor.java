package com.erp.sri_files.processor.guiaremision;

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
public class GuiaRemisionProcessor implements DocumentoProcessor {

    private static final DateTimeFormatter SRI_DATE_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private final ObjectMapper objectMapper;

    public GuiaRemisionProcessor(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public TipoDocumento soporta() {
        return TipoDocumento.GUIA_REMISION;
    }

    @Override
    public void validar(DocumentoElectronico documento) {
        try {
            JsonNode root = objectMapper.readTree(documento.getJsonOriginal());
            require(root, "/documento/fechaInicioTransporte", "documento.fechaInicioTransporte");
            require(root, "/documento/fechaFinTransporte", "documento.fechaFinTransporte");
            require(root, "/documento/direccionPartida", "documento.direccionPartida");
            require(root, "/documento/placa", "documento.placa");
            validateDestinatarios(root);
            validatePlaca(root.at("/documento/placa").asText());
            validateFechasTransporte(
                    root.at("/documento/fechaInicioTransporte").asText(),
                    root.at("/documento/fechaFinTransporte").asText()
            );
        } catch (DocumentoRecepcionException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new DocumentoRecepcionException("No se pudo validar la estructura base de la guia de remision");
        }
    }

    private void require(JsonNode root, String pointer, String field) {
        JsonNode node = root.at(pointer);
        if (node.isMissingNode() || node.isNull() || node.asText().isBlank()) {
            throw new DocumentoRecepcionException("El campo " + field + " es obligatorio para la guia de remision");
        }
    }

    private void requireDetalles(JsonNode detallesNode) {
        if (!detallesNode.isArray() || detallesNode.isEmpty()) {
            throw new DocumentoRecepcionException("La guia de remision debe incluir al menos un detalle");
        }

        JsonNode primerDetalle = detallesNode.get(0);
        if (primerDetalle == null || primerDetalle.isNull()) {
            throw new DocumentoRecepcionException("La guia de remision debe incluir un detalle valido");
        }

        requireNode(primerDetalle, "descripcion", "detalles[0].descripcion");
        requireNode(primerDetalle, "cantidad", "detalles[0].cantidad");

        for (int i = 0; i < detallesNode.size(); i++) {
            validateDetalle(detallesNode.get(i), i);
        }
    }

    private void requireNode(JsonNode parent, String fieldName, String field) {
        JsonNode node = parent.path(fieldName);
        if (node.isMissingNode() || node.isNull() || node.asText().isBlank()) {
            throw new DocumentoRecepcionException("El campo " + field + " es obligatorio para la guia de remision");
        }
    }

    private void validatePlaca(String placa) {
        String normalized = placa == null ? "" : placa.trim().replace("-", "");
        if (!normalized.matches("[A-Za-z]{3}\\d{3,4}")) {
            throw new DocumentoRecepcionException("El campo documento.placa debe tener un formato valido, por ejemplo ABC1234");
        }
    }

    private void validateDestinatarios(JsonNode root) {
        JsonNode destinatariosNode = root.path("destinatarios");
        if (destinatariosNode.isArray() && !destinatariosNode.isEmpty()) {
            for (int i = 0; i < destinatariosNode.size(); i++) {
                validateDestinatario(destinatariosNode.get(i), "destinatarios[" + i + "]");
            }
            return;
        }

        JsonNode receptorNode = root.path("receptor");
        requireNode(receptorNode, "identificacion", "receptor.identificacion");
        requireNode(receptorNode, "razonSocial", "receptor.razonSocial");
        requireNode(receptorNode, "direccion", "receptor.direccion");
        require(root, "/documento/motivoTraslado", "documento.motivoTraslado");
        require(root, "/documento/codDocSustento", "documento.codDocSustento");
        require(root, "/documento/numDocSustento", "documento.numDocSustento");
        validateNumeroDocumentoSustento(root.at("/documento/numDocSustento").asText(), "documento.numDocSustento");
        if (!root.at("/documento/fechaEmisionDocSustento").asText().isBlank()) {
            validateFecha(root.at("/documento/fechaEmisionDocSustento").asText(), "documento.fechaEmisionDocSustento");
        }
        requireDetalles(root.path("detalles"));
    }

    private void validateDestinatario(JsonNode destinatarioNode, String prefix) {
        requireNode(destinatarioNode, "identificacion", prefix + ".identificacion");
        requireNode(destinatarioNode, "razonSocial", prefix + ".razonSocial");
        requireNode(destinatarioNode, "direccion", prefix + ".direccion");
        requireNode(destinatarioNode, "motivoTraslado", prefix + ".motivoTraslado");
        requireNode(destinatarioNode, "codDocSustento", prefix + ".codDocSustento");
        requireNode(destinatarioNode, "numDocSustento", prefix + ".numDocSustento");
        validateNumeroDocumentoSustento(destinatarioNode.path("numDocSustento").asText(), prefix + ".numDocSustento");
        if (!destinatarioNode.path("fechaEmisionDocSustento").asText().isBlank()) {
            validateFecha(destinatarioNode.path("fechaEmisionDocSustento").asText(), prefix + ".fechaEmisionDocSustento");
        }
        requireDetalles(destinatarioNode.path("detalles"));
    }

    private void validateFechasTransporte(String fechaInicio, String fechaFin) {
        LocalDate inicio = validateFecha(fechaInicio, "documento.fechaInicioTransporte");
        LocalDate fin = validateFecha(fechaFin, "documento.fechaFinTransporte");
        if (fin.isBefore(inicio)) {
            throw new DocumentoRecepcionException("documento.fechaFinTransporte no puede ser menor a documento.fechaInicioTransporte");
        }
    }

    private LocalDate validateFecha(String fecha, String field) {
        try {
            return LocalDate.parse(fecha.trim(), SRI_DATE_FORMAT);
        } catch (DateTimeParseException ex) {
            throw new DocumentoRecepcionException("El campo " + field + " debe tener formato dd/MM/yyyy");
        }
    }

    private void validateNumeroDocumentoSustento(String numero, String field) {
        String normalized = numero == null ? "" : numero.trim();
        if (!normalized.matches("\\d{3}-\\d{3}-\\d{9}")) {
            throw new DocumentoRecepcionException("El campo " + field + " debe tener formato 001-001-000000001");
        }
    }

    private void validateDetalle(JsonNode detalleNode, int index) {
        requireNode(detalleNode, "descripcion", "detalles[" + index + "].descripcion");
        requireNode(detalleNode, "cantidad", "detalles[" + index + "].cantidad");
        try {
            BigDecimal cantidad = new BigDecimal(detalleNode.path("cantidad").asText().trim());
            if (cantidad.compareTo(BigDecimal.ZERO) <= 0) {
                throw new DocumentoRecepcionException("El campo detalles[" + index + "].cantidad debe ser mayor a cero");
            }
        } catch (NumberFormatException ex) {
            throw new DocumentoRecepcionException("El campo detalles[" + index + "].cantidad debe ser numerico");
        }
    }
}
