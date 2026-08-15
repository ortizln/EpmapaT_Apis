package com.erp.sri_files.processor.retencion;

import com.erp.sri_files.domain.documento.DocumentoElectronico;
import com.erp.sri_files.domain.documento.TipoDocumento;
import com.erp.sri_files.exceptions.DocumentoRecepcionException;
import com.erp.sri_files.processor.DocumentoProcessor;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

@Component
public class RetencionProcessor implements DocumentoProcessor {

    private final ObjectMapper objectMapper;

    public RetencionProcessor(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public TipoDocumento soporta() {
        return TipoDocumento.RETENCION;
    }

    @Override
    public void validar(DocumentoElectronico documento) {
        try {
            JsonNode root = objectMapper.readTree(documento.getJsonOriginal());
            String xml = readText(root, "/documento/xml");
            if (xml == null) {
                xml = readText(root, "/documento/xmlPlano");
            }
            if (xml == null) {
                xml = readText(root, "/documento/xmlRetencion");
            }
            if (xml == null || xml.isBlank()) {
                throw new DocumentoRecepcionException(
                        "La retencion requiere documento.xml, documento.xmlPlano o documento.xmlRetencion en el payload"
                );
            }
        } catch (DocumentoRecepcionException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new DocumentoRecepcionException("No se pudo validar la estructura base de la retencion");
        }
    }

    private String readText(JsonNode root, String pointer) {
        JsonNode node = root.at(pointer);
        if (node.isMissingNode() || node.isNull()) {
            return null;
        }
        String text = node.asText();
        return text == null || text.isBlank() ? null : text.trim();
    }
}
