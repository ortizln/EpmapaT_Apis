package com.erp.sri_files.dto.response;

public record RolAuditoriaListadoItemResponse(
        Long id,
        String rolCodigo,
        String rolNombre,
        String accion,
        String descripcion,
        String actorUsername,
        String fecha
) {
}
