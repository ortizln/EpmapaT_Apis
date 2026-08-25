package com.erp.sri_files.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record IvaTarifaCatalogoRequest(
        @NotBlank(message = "La empresa es obligatoria")
        String empresaId,
        @NotBlank(message = "El codigo es obligatorio")
        @Size(max = 20, message = "El codigo no puede exceder 20 caracteres")
        String codigo,
        @NotBlank(message = "El nombre es obligatorio")
        @Size(max = 150, message = "El nombre no puede exceder 150 caracteres")
        String nombre,
        @NotNull(message = "El porcentaje es obligatorio")
        @DecimalMin(value = "0.00", message = "El porcentaje no puede ser negativo")
        BigDecimal porcentaje,
        @Size(max = 10, message = "El codigo SRI no puede exceder 10 caracteres")
        String codigoSri,
        @Size(max = 300, message = "La descripcion no puede exceder 300 caracteres")
        String descripcion
) {
}
