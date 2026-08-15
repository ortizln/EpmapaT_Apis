package com.erp.sri_files.dto.response;

public record DashboardTiemposResponse(
        long promedioProcesamientoMs,
        long promedioAutorizacionMs
) {
}
