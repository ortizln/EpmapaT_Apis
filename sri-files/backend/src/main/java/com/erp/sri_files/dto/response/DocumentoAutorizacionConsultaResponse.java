package com.erp.sri_files.dto.response;

public record DocumentoAutorizacionConsultaResponse(
        String claveAcceso,
        String estado,
        boolean autorizado,
        String numeroAutorizacion,
        String fechaAutorizacion,
        String mensaje,
        boolean encontrada,
        String xmlAutorizado
) {
}
