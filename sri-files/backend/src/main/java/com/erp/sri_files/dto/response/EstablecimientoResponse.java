package com.erp.sri_files.dto.response;

public record EstablecimientoResponse(
        String id,
        String empresaId,
        String empresaRazonSocial,
        String codigo,
        String nombre,
        String direccion,
        boolean activo
) {
}
