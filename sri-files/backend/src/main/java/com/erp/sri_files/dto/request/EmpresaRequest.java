package com.erp.sri_files.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record EmpresaRequest(
        @NotBlank(message = "El RUC es obligatorio")
        @Pattern(regexp = "\\d{13}", message = "El RUC debe tener 13 digitos")
        String ruc,
        @NotBlank(message = "La razon social es obligatoria")
        @Size(max = 300, message = "La razon social no puede exceder 300 caracteres")
        String razonSocial,
        @Size(max = 300, message = "El nombre comercial no puede exceder 300 caracteres")
        String nombreComercial,
        @Size(max = 500, message = "La direccion matriz no puede exceder 500 caracteres")
        String direccionMatriz,
        boolean obligadoContabilidad,
        @Size(max = 50, message = "El contribuyente especial no puede exceder 50 caracteres")
        String contribuyenteEspecial
) {
}
