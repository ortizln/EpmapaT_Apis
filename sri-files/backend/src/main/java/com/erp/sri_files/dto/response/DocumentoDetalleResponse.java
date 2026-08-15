package com.erp.sri_files.dto.response;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record DocumentoDetalleResponse(
        String id,
        String tipoDocumento,
        String estado,
        String externalId,
        String numeroDocumento,
        String claveAcceso,
        String identificacionReceptor,
        String razonSocialReceptor,
        String emailReceptor,
        LocalDate fechaEmision,
        LocalDateTime fechaRecepcion
) {
}
