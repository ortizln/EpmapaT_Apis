package com.erp.sri_files.service;

import com.erp.sri_files.domain.sri.SriIntentoComunicacion;
import com.erp.sri_files.dto.response.SriFlujoResumenResponse;
import com.erp.sri_files.dto.response.SriIntentoResponse;
import com.erp.sri_files.repositories.FacturaR;
import com.erp.sri_files.repositories.sri.SriIntentoRepository;
import com.erp.sri_files.sri.circuitbreaker.SriCircuitBreaker;
import com.erp.sri_files.sri.metricas.SriMetricasRegistry;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Arma el resumen de supervisión del flujo SRI combinando el histórico de BD
 * (sri_intento_comunicacion + fec_factura) con el estado en memoria del proceso
 * (contadores y circuit breaker).
 */
@Service
public class SriFlujoService {

    private final SriIntentoRepository intentoRepository;
    private final FacturaR facturaR;
    private final SriMetricasRegistry metricasRegistry;
    private final SriCircuitBreaker circuitBreaker;

    public SriFlujoService(SriIntentoRepository intentoRepository,
                           FacturaR facturaR,
                           SriMetricasRegistry metricasRegistry,
                           SriCircuitBreaker circuitBreaker) {
        this.intentoRepository = intentoRepository;
        this.facturaR = facturaR;
        this.metricasRegistry = metricasRegistry;
        this.circuitBreaker = circuitBreaker;
    }

    public SriFlujoResumenResponse resumen() {
        LocalDateTime ahora = LocalDateTime.now();
        return new SriFlujoResumenResponse(
                intentoRepository.count(),
                toMap(intentoRepository.contarPorResultado()),
                toMap(intentoRepository.contarPorServicio()),
                toMap(intentoRepository.contarPorTipoError()),
                intentoRepository.promedioDuracionMs(),
                intentoRepository.contarPendientesReintento(ahora),
                toMap(facturaR.contarPorEstadoDocumento()),
                circuitBreaker.snapshot(),
                toIntentos(intentoRepository.findTop50ByOrderByFechaFinDesc()),
                metricasRegistry.snapshot());
    }

    private static Map<String, Long> toMap(List<Object[]> rows) {
        Map<String, Long> out = new LinkedHashMap<>();
        if (rows == null) return out;
        for (Object[] r : rows) {
            if (r == null || r.length < 2 || r[0] == null) continue;
            String key = String.valueOf(r[0]);
            long val = r[1] instanceof Number n ? n.longValue() : 0L;
            out.merge(key, val, Long::sum);
        }
        return out;
    }

    private static List<SriIntentoResponse> toIntentos(List<SriIntentoComunicacion> list) {
        if (list == null) return List.of();
        return list.stream()
                .map(i -> new SriIntentoResponse(
                        i.getId(), i.getDocumentoId(), i.getClaveAcceso(), i.getServicio(),
                        i.getResultado(), i.getCodigoSri(), i.getMensajeSri(), i.getTipoError(),
                        i.getNumeroIntento(), i.getFechaFin(), i.isReintentable(),
                        i.isRequiereConsulta(), i.getWorkerId()))
                .toList();
    }
}