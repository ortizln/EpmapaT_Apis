package com.erp.sri_files.dto.request;

import jakarta.validation.constraints.Min;

public record SecuencialRequest(
        @Min(value = 0, message = "El valor actual no puede ser negativo")
        long valorActual,
        boolean activo
) {
}
