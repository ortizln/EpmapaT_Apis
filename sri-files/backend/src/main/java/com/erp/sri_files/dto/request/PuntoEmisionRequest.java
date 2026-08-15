package com.erp.sri_files.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record PuntoEmisionRequest(
        @NotBlank(message = "El codigo es obligatorio")
        @Pattern(regexp = "\\d{3}", message = "El codigo debe tener 3 digitos")
        String codigo,
        @Size(max = 200, message = "El nombre no puede exceder 200 caracteres")
        String nombre
) {
}
