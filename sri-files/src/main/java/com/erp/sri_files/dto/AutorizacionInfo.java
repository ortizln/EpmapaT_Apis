package com.erp.sri_files.dto;

// DTO para tu servicio
public record AutorizacionInfo(
        boolean autorizado,
        String numeroAutorizacion,
        java.time.LocalDateTime fechaAutorizacion,
        byte[] xmlAutorizado,
        String mensajesConcatenados
) {}