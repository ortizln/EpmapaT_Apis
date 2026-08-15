package com.erp.sri_files.dto.response;

public record DocumentoEstadoResponse(
        String id,
        String estado,
        boolean requiereIntervencion
) {
}
