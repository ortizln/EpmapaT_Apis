package com.erp.sri_files.dto.response;

public record DocumentoHistorialItemResponse(
        Long id,
        String estadoAnterior,
        String estadoNuevo,
        String descripcion,
        String origen,
        Long usuarioId,
        String metadata,
        String createdAt
) {
}
