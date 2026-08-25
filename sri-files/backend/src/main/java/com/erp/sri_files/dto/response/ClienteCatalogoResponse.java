package com.erp.sri_files.dto.response;

public record ClienteCatalogoResponse(
        String id,
        String empresaId,
        String tipoIdentificacion,
        String identificacion,
        String razonSocial,
        String nombreComercial,
        String email,
        String telefono,
        String direccion,
        String observacion,
        boolean activo
) {
}
