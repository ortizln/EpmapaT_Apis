package com.erp.sri_files.dto.response;

public record CertificadoResponse(
        String uuid,
        String empresaId,
        String nombre,
        boolean activo,
        boolean valido,
        String alias,
        String titular,
        String fechaEmision,
        String fechaExpiracion,
        Long diasRestantes
) {
}
