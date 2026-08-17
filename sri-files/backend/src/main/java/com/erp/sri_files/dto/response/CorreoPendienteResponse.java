package com.erp.sri_files.dto.response;

import java.util.List;

public record CorreoPendienteResponse(
        long total,
        List<CorreoPendienteItemResponse> items
) {
}
