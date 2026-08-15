package com.erp.sri_files.dto.response;

public record UsuarioAuditoriaResponse(
        String accion,
        String descripcion,
        String actorUsername,
        String fecha
) {
}
