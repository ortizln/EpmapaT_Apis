package com.erp.sri_files.dto.response;

import java.util.List;

public record DocumentoListadoResponse(
        List<DocumentoListadoItemResponse> items,
        int page,
        int size,
        long totalItems,
        int totalPages
) {
}
