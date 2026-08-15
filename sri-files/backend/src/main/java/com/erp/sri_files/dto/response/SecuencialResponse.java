package com.erp.sri_files.dto.response;

public record SecuencialResponse(
        String puntoEmisionId,
        String tipoDocumento,
        long valorActual,
        boolean activo
) {
}
