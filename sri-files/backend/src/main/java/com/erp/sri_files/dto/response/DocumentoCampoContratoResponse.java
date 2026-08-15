package com.erp.sri_files.dto.response;

public record DocumentoCampoContratoResponse(
        String nombre,
        String tipo,
        boolean requerido,
        String descripcion,
        String ejemplo
) {
}
