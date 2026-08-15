package com.erp.sri_files.dto.response;

public record LoginResponse(
        String accessToken,
        String refreshToken,
        UsuarioAutenticadoResponse usuario
) {
}
