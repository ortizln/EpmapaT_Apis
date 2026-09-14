package com.erp.sri_files.sri.metricas;

import java.time.LocalDateTime;

/**
 * Evento reciente de comunicación con el SRI capturado en memoria
 * (del proceso actual, no histórico).
 */
public record SriEventoReciente(
        LocalDateTime fecha,
        String servicio,
        String resultado,
        long duracionMs) {
}