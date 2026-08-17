package com.erp.sri_files.dto.response;

public record MonitorResumenResponse(
        long totalDocumentos,
        long pendientesProcesamiento,
        long pendientesAutorizacion,
        long pendientesCorreo,
        long conError,
        long finalizados
) {
}
