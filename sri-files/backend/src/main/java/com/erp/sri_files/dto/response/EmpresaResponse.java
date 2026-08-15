package com.erp.sri_files.dto.response;

public record EmpresaResponse(
        String id,
        String ruc,
        String razonSocial,
        String nombreComercial,
        String direccionMatriz,
        boolean obligadoContabilidad,
        String contribuyenteEspecial,
        int ambienteSri,
        String correoNotificaciones,
        boolean certificadoConfigurado,
        boolean activo
) {
}
