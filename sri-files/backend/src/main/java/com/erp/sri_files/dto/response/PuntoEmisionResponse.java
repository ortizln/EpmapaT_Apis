package com.erp.sri_files.dto.response;

public record PuntoEmisionResponse(
        String id,
        String establecimientoId,
        String establecimientoCodigo,
        String empresaId,
        String empresaRazonSocial,
        String codigo,
        String nombre,
        boolean activo
) {
}
