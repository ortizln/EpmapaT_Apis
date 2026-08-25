package com.erp.sri_files.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public record RolCrearRequest(
        @NotBlank String codigo,
        @NotBlank String nombre,
        @NotBlank String descripcion,
        @NotEmpty List<@NotBlank String> permisos
) {
}
