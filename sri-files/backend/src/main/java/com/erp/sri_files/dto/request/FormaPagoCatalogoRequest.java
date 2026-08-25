package com.erp.sri_files.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record FormaPagoCatalogoRequest(
        @NotBlank(message = "La empresa es obligatoria")
        String empresaId,
        @NotBlank(message = "El codigo es obligatorio")
        @Size(max = 20, message = "El codigo no puede exceder 20 caracteres")
        String codigo,
        @NotBlank(message = "El nombre es obligatorio")
        @Size(max = 150, message = "El nombre no puede exceder 150 caracteres")
        String nombre,
        @Size(max = 300, message = "La descripcion no puede exceder 300 caracteres")
        String descripcion,
        @Min(value = 0, message = "Los dias plazo no pueden ser negativos")
        int diasPlazo
) {
}
