package com.erp.sri_files.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ClienteCatalogoRequest(
        @NotBlank(message = "La empresa es obligatoria")
        String empresaId,
        @NotBlank(message = "El tipo de identificacion es obligatorio")
        @Size(max = 2, message = "El tipo de identificacion no puede exceder 2 caracteres")
        String tipoIdentificacion,
        @NotBlank(message = "La identificacion es obligatoria")
        @Size(max = 20, message = "La identificacion no puede exceder 20 caracteres")
        String identificacion,
        @NotBlank(message = "La razon social es obligatoria")
        @Size(max = 300, message = "La razon social no puede exceder 300 caracteres")
        String razonSocial,
        @Size(max = 300, message = "El nombre comercial no puede exceder 300 caracteres")
        String nombreComercial,
        @Size(max = 320, message = "El email no puede exceder 320 caracteres")
        String email,
        @Size(max = 30, message = "El telefono no puede exceder 30 caracteres")
        String telefono,
        @Size(max = 500, message = "La direccion no puede exceder 500 caracteres")
        String direccion,
        @Size(max = 500, message = "La observacion no puede exceder 500 caracteres")
        String observacion
) {
}
