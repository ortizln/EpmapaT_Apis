package com.erp.sri_files.dto.request;

import jakarta.validation.constraints.NotBlank;

public record UsuarioPasswordResetRequest(
        @NotBlank String password
) {
}
