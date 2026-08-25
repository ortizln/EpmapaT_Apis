package com.erp.sri_files.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;

public record CorreoConfiguracionRequest(
        @Email(message = "El remitente debe ser un correo valido")
        @Size(max = 320, message = "El remitente no puede exceder 320 caracteres")
        String remitente,
        @Size(max = 255, message = "El nombre del remitente no puede exceder 255 caracteres")
        String nombreRemitente,
        boolean enviarXml,
        boolean enviarRide,
        @Size(max = 255, message = "La plantilla del asunto no puede exceder 255 caracteres")
        String plantillaAsunto
) {
}
