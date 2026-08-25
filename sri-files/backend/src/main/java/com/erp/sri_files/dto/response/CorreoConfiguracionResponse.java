package com.erp.sri_files.dto.response;

public record CorreoConfiguracionResponse(
        String empresaId,
        String remitente,
        String nombreRemitente,
        boolean enviarXml,
        boolean enviarRide,
        String plantillaAsunto
) {
}
