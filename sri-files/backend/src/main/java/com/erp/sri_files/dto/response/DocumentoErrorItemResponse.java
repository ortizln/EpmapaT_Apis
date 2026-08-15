package com.erp.sri_files.dto.response;

public record DocumentoErrorItemResponse(
        Long id,
        String etapa,
        String codigo,
        String mensaje,
        String detalle,
        boolean recuperable,
        boolean resuelto,
        String fechaResolucion,
        String createdAt
) {
}
