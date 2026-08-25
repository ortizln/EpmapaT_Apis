package com.erp.sri_files.dto.response;

public record DocumentoOperacionManualResponse(
        String id,
        String estadoAnterior,
        String estado,
        String accion,
        String mensaje
) {
}
