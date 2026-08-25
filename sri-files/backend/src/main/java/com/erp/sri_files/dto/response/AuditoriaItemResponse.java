package com.erp.sri_files.dto.response;

public record AuditoriaItemResponse(
        String id,
        String entidad,
        String entidadId,
        String accion,
        String descripcion,
        String usuario,
        String fecha
) {
}
