package com.erp.sri_files.dto.response;

import java.util.List;

public record MonitorPendientesResponse(
        long total,
        List<MonitorPendienteItemResponse> items
) {
}
