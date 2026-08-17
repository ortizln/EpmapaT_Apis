package com.erp.sri_files.dto.response;

import java.util.List;

public record UsuarioAuditoriaListadoResponse(
        List<UsuarioAuditoriaListadoItemResponse> items,
        int page,
        int size,
        long totalItems,
        int totalPages
) {
}
