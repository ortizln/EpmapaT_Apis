package com.erp.sri_files.dto.response;

public record DocumentoRecepcionResponse(
        String id,
        String tipoDocumento,
        String estado,
        String mensaje,
        boolean duplicado
) {
}
