package com.erp.sri_files.sri.metricas;

import java.util.List;
import java.util.Map;

/**
 * Instante de los contadores en memoria de comunicaciones con el SRI.
 */
public record SriMetricasSnapshot(
        long total,
        Map<String, Long> porResultado,
        Map<String, Long> porServicio,
        Map<String, Long> porEstadoComunicacion,
        List<SriEventoReciente> ultimosEventos) {
}