package com.erp.sri_files.dto.response;

public record UsuarioSistemaResponse(
        String id,
        String username,
        String nombre,
        String correo,
        String rol,
        boolean activo
) {
}
