package com.erp.sri_files.dto.response;

public record DocumentoCorreoEventoResponse(
        String tipo,
        String estado,
        String resultado,
        String descripcion,
        String codigo,
        String mensaje,
        boolean recuperable,
        String createdAt
) {
}
