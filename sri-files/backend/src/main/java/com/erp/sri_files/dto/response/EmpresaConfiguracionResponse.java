package com.erp.sri_files.dto.response;

public record EmpresaConfiguracionResponse(
        String empresaId,
        int ambienteSri,
        String correoNotificaciones,
        String correoRespuesta,
        boolean certificadoConfigurado,
        String certificadoNombre,
        String certificadoAlias,
        String certificadoTitular,
        String certificadoVigenciaDesde,
        String certificadoVigenciaHasta
) {
}
