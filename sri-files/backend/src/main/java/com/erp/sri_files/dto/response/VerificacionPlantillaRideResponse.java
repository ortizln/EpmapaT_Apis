package com.erp.sri_files.dto.response;

public record VerificacionPlantillaRideResponse(
        String plantillaId,
        boolean valida,
        String mensaje,
        String tipoDocumento,
        String nombreArchivo
) {
}
