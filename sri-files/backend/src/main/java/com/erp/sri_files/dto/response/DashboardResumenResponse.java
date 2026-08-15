package com.erp.sri_files.dto.response;

public record DashboardResumenResponse(
        long total,
        long recibidos,
        long procesando,
        long autorizados,
        long noAutorizados,
        long errores,
        long correosPendientes
) {
}
