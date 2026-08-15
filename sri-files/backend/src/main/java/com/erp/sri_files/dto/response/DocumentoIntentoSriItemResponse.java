package com.erp.sri_files.dto.response;

public record DocumentoIntentoSriItemResponse(
        String tipo,
        String etapa,
        String estado,
        String resultado,
        String descripcion,
        String codigo,
        String mensaje,
        boolean recuperable,
        String createdAt
) {
}
