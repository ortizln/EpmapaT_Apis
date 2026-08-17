package com.erp.sri_files.dto.response;

import java.util.List;

public record RolResponse(
        String codigo,
        String nombre,
        String descripcion,
        List<String> permisos
) {
}
