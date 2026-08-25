package com.erp.sri_files.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record ProductoCatalogoRequest(
        @NotBlank(message = "La empresa es obligatoria")
        String empresaId,
        @NotBlank(message = "El codigo es obligatorio")
        @Size(max = 60, message = "El codigo no puede exceder 60 caracteres")
        String codigo,
        @NotBlank(message = "El nombre es obligatorio")
        @Size(max = 300, message = "El nombre no puede exceder 300 caracteres")
        String nombre,
        @Size(max = 500, message = "La descripcion no puede exceder 500 caracteres")
        String descripcion,
        @Size(max = 20, message = "La unidad de medida no puede exceder 20 caracteres")
        String unidadMedida,
        @NotNull(message = "El precio base es obligatorio")
        @DecimalMin(value = "0.00", message = "El precio base no puede ser negativo")
        BigDecimal precioBase,
        @NotNull(message = "El porcentaje IVA es obligatorio")
        @DecimalMin(value = "0.00", message = "El porcentaje IVA no puede ser negativo")
        BigDecimal porcentajeIva
) {
}
