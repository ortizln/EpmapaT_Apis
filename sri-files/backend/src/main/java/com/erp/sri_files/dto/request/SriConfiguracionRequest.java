package com.erp.sri_files.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record SriConfiguracionRequest(
        @NotNull(message = "El ambiente es obligatorio")
        @Min(value = 1, message = "El ambiente SRI debe ser 1 o 2")
        @Max(value = 2, message = "El ambiente SRI debe ser 1 o 2")
        Integer ambiente,
        @NotNull(message = "El timeout de conexion es obligatorio")
        @Min(value = 1000, message = "El timeout de conexion debe ser al menos 1000 ms")
        Integer timeoutConexionMs,
        @NotNull(message = "El timeout de respuesta es obligatorio")
        @Min(value = 1000, message = "El timeout de respuesta debe ser al menos 1000 ms")
        Integer timeoutRespuestaMs,
        @NotNull(message = "El maximo de reintentos es obligatorio")
        @Min(value = 0, message = "El maximo de reintentos no puede ser negativo")
        Integer maxReintentos,
        @NotNull(message = "El estado activo es obligatorio")
        Boolean activo
) {
}
