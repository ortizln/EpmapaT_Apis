package com.erp.sri_files.dto.response;

import java.util.List;

public record AuditoriaListadoResponse(
        List<AuditoriaItemResponse> content,
        int page,
        int size,
        long totalElements,
        int totalPages
) {
}
