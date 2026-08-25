package com.erp.sri_files.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record UsuarioActualizarRequest(
        @NotBlank String nombre,
        @Email @NotBlank String correo,
        @NotBlank String rol
) {
}
