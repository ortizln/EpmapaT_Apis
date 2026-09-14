package com.erp.sri_files.sri.metricas;

import com.erp.sri_files.sri.model.ResultadoSri;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.LongAdder;

/**
 * Contadores en memoria de las comunicaciones con el SRI del proceso actual.
 * Complementa el histórico de BD (sri_intento_comunicacion); se reinicia en cada
 * arranque y es local a cada instancia del servicio.
 */
@Component
public class SriMetricasRegistry {

    private static final int MAX_EVENTOS_RECIENTES = 50;

    private final LongAdder total = new LongAdder();
    private final ConcurrentHashMap<String, LongAdder> porResultado = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, LongAdder> porServicio = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, LongAdder> porEstadoComunicacion = new ConcurrentHashMap<>();
    private final Queue<SriEventoReciente> recientes = new ConcurrentLinkedQueue<>();

    public void acumular(ResultadoSri resultado, String servicio) {
        total.increment();
        String resultKey = resultado == null || resultado.getTipo() == null
                ? "SIN_TIPO" : resultado.getTipo().name();
        increment(porResultado, resultKey);
        increment(porServicio, servicio == null ? "DESCONOCIDO" : servicio);
        if (resultado != null && resultado.getEstadoComunicacion() != null) {
            increment(porEstadoComunicacion, resultado.getEstadoComunicacion().name());
        }
        long duracion = resultado == null ? 0L : resultado.getDuracionMs();
        recientes.add(new SriEventoReciente(java.time.LocalDateTime.now(), toServiceKey(servicio), resultKey, duracion));
        while (recientes.size() > MAX_EVENTOS_RECIENTES) {
            recientes.poll();
        }
    }

    public SriMetricasSnapshot snapshot() {
        return new SriMetricasSnapshot(
                total.sum(),
                snapshotOf(porResultado),
                snapshotOf(porServicio),
                snapshotOf(porEstadoComunicacion),
                List.copyOf(recientes));
    }

    private static String toServiceKey(String servicio) {
        return servicio == null ? "DESCONOCIDO" : servicio;
    }

    private static void increment(ConcurrentHashMap<String, LongAdder> map, String key) {
        map.computeIfAbsent(key, k -> new LongAdder()).increment();
    }

    private static Map<String, Long> snapshotOf(ConcurrentHashMap<String, LongAdder> map) {
        Map<String, Long> out = new java.util.TreeMap<>();
        map.forEach((k, v) -> out.put(k, v.sum()));
        return out;
    }
}