package com.erp.sri_files.dto.response;

import java.math.BigDecimal;

public record ProductoCatalogoResponse(
        String id,
        String empresaId,
        String codigo,
        String nombre,
        String descripcion,
        String unidadMedida,
        BigDecimal precioBase,
        BigDecimal porcentajeIva,
        boolean activo
) {
}
