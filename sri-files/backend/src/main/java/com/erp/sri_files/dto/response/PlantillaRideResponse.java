package com.erp.sri_files.dto.response;

public record PlantillaRideResponse(
        String uuid,
        String empresaId,
        String tipoDocumento,
        String nombre,
        String version,
        boolean predeterminada,
        boolean activa,
        String nombreArchivo,
        String createdAt
) {
}
