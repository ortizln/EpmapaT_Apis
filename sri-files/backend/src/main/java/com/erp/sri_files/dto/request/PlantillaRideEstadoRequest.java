package com.erp.sri_files.dto.request;

import jakarta.validation.constraints.NotNull;

public record PlantillaRideEstadoRequest(
        @NotNull(message = "El estado activo es obligatorio")
        Boolean activa
) {
}
