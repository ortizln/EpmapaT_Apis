package com.erp.sri_files.dto.response;

public record DocumentoAuditoriaEventoResponse(
        Long id,
        String documentoUuid,
        String tipoDocumento,
        String numeroDocumento,
        String externalId,
        String estadoAnterior,
        String estadoNuevo,
        String descripcion,
        String origen,
        String createdAt
) {
}
