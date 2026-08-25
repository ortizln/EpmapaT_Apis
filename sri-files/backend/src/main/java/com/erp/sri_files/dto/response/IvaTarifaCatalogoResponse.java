package com.erp.sri_files.dto.response;

import java.math.BigDecimal;

public record IvaTarifaCatalogoResponse(
        String id,
        String empresaId,
        String codigo,
        String nombre,
        BigDecimal porcentaje,
        String codigoSri,
        String descripcion,
        boolean activo
) {
}
