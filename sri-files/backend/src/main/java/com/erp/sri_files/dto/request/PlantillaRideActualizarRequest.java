package com.erp.sri_files.dto.request;

import com.erp.sri_files.domain.documento.TipoDocumento;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record PlantillaRideActualizarRequest(
        @NotNull(message = "El tipo de documento es obligatorio")
        TipoDocumento tipoDocumento,
        @NotBlank(message = "El nombre es obligatorio")
        @Size(max = 255, message = "El nombre no puede exceder 255 caracteres")
        String nombre,
        @NotBlank(message = "La version es obligatoria")
        @Size(max = 50, message = "La version no puede exceder 50 caracteres")
        String version,
        boolean predeterminada,
        boolean activa
) {
}
