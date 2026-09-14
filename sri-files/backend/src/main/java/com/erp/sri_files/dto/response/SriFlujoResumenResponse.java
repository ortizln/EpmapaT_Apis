package com.erp.sri_files.dto.response;

import com.erp.sri_files.sri.metricas.SriMetricasSnapshot;

import java.util.List;
import java.util.Map;

/**
 * Resumen de supervisión del flujo SRI: histórico en BD (sri_intento_comunicacion
 * y fec_factura) + estado en memoria (registro del proceso + circuit breaker).
 */
public record SriFlujoResumenResponse(
        long totalIntentos,
        Map<String, Long> porResultado,
        Map<String, Long> porServicio,
        Map<String, Long> porTipoError,
        Double promedioDuracionMs,
        long pendientesReintento,
        Map<String, Long> porEstadoDocumento,
        Map<String, String> circuitoPorServicio,
        List<SriIntentoResponse> ultimosIntentos,
        SriMetricasSnapshot enMemoria) {
}