package com.erp.sri_files.dto.response;

public record EmpresaAuditoriaListadoItemResponse(
        Long id,
        String empresaId,
        String ruc,
        String razonSocial,
        String accion,
        String descripcion,
        String actorUsername,
        String fecha
) {
}
