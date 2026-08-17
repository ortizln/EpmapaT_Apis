package com.erp.sri_files.dto.response;

import java.util.List;

public record UsuarioAutenticadoResponse(
        String id,
        String nombre,
        String correo,
        List<String> roles,
        List<String> permisos
) {
}
