package com.erp.sri_files.dto.response;

public record RecursoEmpresaResponse(
        String uuid,
        String empresaId,
        String tipo,
        String nombre,
        String nombreArchivo,
        String mimeType,
        boolean activo,
        String createdAt
) {
}
