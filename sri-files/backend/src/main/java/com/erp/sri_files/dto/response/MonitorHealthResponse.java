package com.erp.sri_files.dto.response;

import java.util.List;

public record MonitorHealthResponse(
        String estado,
        String timestamp,
        MonitorResumenResponse resumen,
        List<MonitorComponenteEstadoResponse> componentes
) {
}
