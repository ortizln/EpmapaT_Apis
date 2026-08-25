package com.erp.sri_files.dto.response;

public record VerificacionCertificadoResponse(
        boolean valido,
        String fechaEmision,
        String fechaExpiracion,
        Long diasRestantes
) {
}
