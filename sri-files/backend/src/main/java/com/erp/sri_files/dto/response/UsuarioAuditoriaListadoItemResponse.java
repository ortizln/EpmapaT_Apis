package com.erp.sri_files.dto.response;

public record UsuarioAuditoriaListadoItemResponse(
        Long id,
        String usuarioId,
        String username,
        String nombre,
        String correo,
        String rol,
        String accion,
        String descripcion,
        String actorUsername,
        String fecha
) {
}
