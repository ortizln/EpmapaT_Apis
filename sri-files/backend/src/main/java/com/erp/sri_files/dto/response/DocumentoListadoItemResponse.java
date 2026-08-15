package com.erp.sri_files.dto.response;

public record DocumentoListadoItemResponse(
        String id,
        String tipoDocumento,
        String numeroDocumento,
        String razonSocial,
        String fechaEmision,
        String estado
) {
}
