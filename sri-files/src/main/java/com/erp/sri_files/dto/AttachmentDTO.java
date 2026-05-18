package com.erp.sri_files.dto;

// Adjuntos genéricos
public record AttachmentDTO(
        @jakarta.validation.constraints.NotBlank String filename,
        @jakarta.validation.constraints.NotBlank String mimeType,
        @jakarta.validation.constraints.NotBlank String base64
) {}