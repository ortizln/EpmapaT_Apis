package com.erp.sri_files.dto.response;

import java.time.LocalDateTime;

/**
 * Vista de supervisión de un intento de comunicación con el SRI.
 */
public record SriIntentoResponse(
        Long id,
        Long documentoId,
        String claveAcceso,
        String servicio,
        String resultado,
        String codigoSri,
        String mensajeSri,
        String tipoError,
        Integer numeroIntento,
        LocalDateTime fechaFin,
        boolean reintentable,
        boolean requiereConsulta,
        String workerId) {
}