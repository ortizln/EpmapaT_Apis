package com.erp.sri_files.dto.response;

import java.util.List;

public record ApiErrorResponse(
        String timestamp,
        int status,
        String code,
        String message,
        List<String> details
) {
}
