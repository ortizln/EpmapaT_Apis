package com.erp.sri_files.dto.response;

public record MonitorPendienteItemResponse(
        String uuid,
        String tipoDocumento,
        String numeroDocumento,
        String razonSocial,
        String estado,
        String fechaRecepcion,
        int intentos,
        boolean requiereIntervencion
) {
}
