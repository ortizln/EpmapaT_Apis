package com.erp.sri_files.dto.response;

public record FormaPagoCatalogoResponse(
        String id,
        String empresaId,
        String codigo,
        String nombre,
        String descripcion,
        int diasPlazo,
        boolean activo
) {
}
