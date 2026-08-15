package com.erp.sri_files.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record EmpresaConfiguracionRequest(
        @NotNull(message = "El ambiente SRI es obligatorio")
        @Min(value = 1, message = "El ambiente SRI debe ser 1 o 2")
        @Max(value = 2, message = "El ambiente SRI debe ser 1 o 2")
        Integer ambienteSri,
        @Size(max = 320, message = "El correo de notificaciones no puede exceder 320 caracteres")
        String correoNotificaciones,
        @Size(max = 320, message = "El correo de respuesta no puede exceder 320 caracteres")
        String correoRespuesta,
        @Size(max = 255, message = "El nombre del certificado no puede exceder 255 caracteres")
        String certificadoNombre,
        String certificadoBase64,
        String certificadoClave,
        boolean limpiarCertificado
) {
}
