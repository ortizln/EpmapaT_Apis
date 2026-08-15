package com.erp.sri_files.dto.request;

import jakarta.validation.constraints.NotBlank;

import java.util.List;
import java.util.Map;

public record DocumentoRecepcionRequest(
        @NotBlank String tipoDocumento,
        String externalId,
        Map<String, Object> emisor,
        Map<String, Object> receptor,
        Map<String, Object> documento,
        List<Map<String, Object>> detalles,
        List<Map<String, Object>> destinatarios,
        List<Map<String, Object>> motivos,
        List<Map<String, Object>> impuestos,
        Map<String, Object> informacionAdicional,
        CorreoRequest correo
) {
    public DocumentoRecepcionRequest(
            String tipoDocumento,
            String externalId,
            Map<String, Object> emisor,
            Map<String, Object> receptor,
            Map<String, Object> documento,
            List<Map<String, Object>> detalles,
            List<Map<String, Object>> impuestos,
            Map<String, Object> informacionAdicional,
            CorreoRequest correo
    ) {
        this(
                tipoDocumento,
                externalId,
                emisor,
                receptor,
                documento,
                detalles,
                List.of(),
                List.of(),
                impuestos,
                informacionAdicional,
                correo
        );
    }

    public record CorreoRequest(
            Boolean enviar,
            List<String> destinatarios
    ) {
    }
}
