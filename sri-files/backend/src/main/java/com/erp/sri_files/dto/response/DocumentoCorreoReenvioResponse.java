package com.erp.sri_files.dto.response;

public record DocumentoCorreoReenvioResponse(
        String id,
        String estado,
        String destinatario,
        String mensaje
) {
}
