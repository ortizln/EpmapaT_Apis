package com.erp.sri_files.dto.response;

public record CorreoPendienteItemResponse(
        String uuid,
        String tipoDocumento,
        String numeroDocumento,
        String razonSocial,
        String destinatario,
        String estado,
        String fechaRecepcion,
        String fechaAutorizacion,
        boolean requiereIntervencion
) {
}
