package com.erp.sri_files.dto.response;

public record DocumentoAutorizacionManualResponse(
        String id,
        String claveAcceso,
        String estado,
        boolean autorizado,
        String numeroAutorizacion,
        String fechaAutorizacion,
        String mensaje,
        boolean actualizado
) {
}
