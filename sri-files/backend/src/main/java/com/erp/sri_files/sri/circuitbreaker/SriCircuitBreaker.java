package com.erp.sri_files.sri.circuitbreaker;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Circuit breaker liviano (sin dependencias externas) para proteger al SRI
 * cuando está caído o degradado. Estados por servicio+ambiente:
 * CERRADO, ABIERTO (espera openMillis), SEMI_ABIERTO (sondeo limitado).
 */
@Component
public class SriCircuitBreaker {

    private final boolean enabled;
    private final int failureThreshold;
    private final long openMillis;
    private final int halfOpenMaxCalls;

    public SriCircuitBreaker(
            @Value("${sri.circuit-breaker.enabled:false}") boolean enabled,
            @Value("${sri.circuit-breaker.failure-threshold:5}") int failureThreshold,
            @Value("${sri.circuit-breaker.open-millis:60000}") long openMillis,
            @Value("${sri.circuit-breaker.half-open-max-calls:1}") int halfOpenMaxCalls) {
        this.enabled = enabled;
        this.failureThreshold = Math.max(1, failureThreshold);
        this.openMillis = Math.max(1000L, openMillis);
        this.halfOpenMaxCalls = Math.max(1, halfOpenMaxCalls);
    }

    public enum Estado {
        CERRADO, ABIERTO, SEMI_ABIERTO
    }

    private static final class Slot {
        Estado estado = Estado.CERRADO;
        int fallosConsecutivos;
        int llamadasSemiAbierto;
        long abiertoDesdeMs;
    }

    private final ConcurrentHashMap<String, Slot> slots = new ConcurrentHashMap<>();

    /** ¿Se permite ejecutar la llamada SRI para este servicio? */
    public boolean puedeProceder(String servicio) {
        if (!enabled) return true;
        Slot s = slots.computeIfAbsent(servicio, k -> new Slot());
        synchronized (s) {
            long now = System.currentTimeMillis();
            switch (s.estado) {
                case CERRADO:
                    return true;
                case ABIERTO:
                    if (now - s.abiertoDesdeMs >= openMillis) {
                        s.estado = Estado.SEMI_ABIERTO;
                        s.fallosConsecutivos = 0;
                        s.llamadasSemiAbierto = 1; // este sondeo consume la cuota
                        return true;
                    }
                    return false;
                case SEMI_ABIERTO:
                    return s.llamadasSemiAbierto < halfOpenMaxCalls;
            }
            return true;
        }
    }

    /** Una llamada exitosa: si estábamos en sondeo, el servicio se considera sano. */
    public void onExito(String servicio) {
        if (!enabled) return;
        Slot s = slots.computeIfAbsent(servicio, k -> new Slot());
        synchronized (s) {
            s.fallosConsecutivos = 0;
            if (s.estado == Estado.SEMI_ABIERTO) {
                s.estado = Estado.CERRADO;
            }
        }
    }

    /** Una llamada fallida (transitoria): suma fallos; puede abrir el circuito. */
    public void onFallo(String servicio) {
        if (!enabled) return;
        Slot s = slots.computeIfAbsent(servicio, k -> new Slot());
        synchronized (s) {
            s.fallosConsecutivos++;
            switch (s.estado) {
                case CERRADO:
                    if (s.fallosConsecutivos >= failureThreshold) {
                        s.estado = Estado.ABIERTO;
                        s.abiertoDesdeMs = System.currentTimeMillis();
                    }
                    break;
                case SEMI_ABIERTO:
                    s.estado = Estado.ABIERTO;
                    s.abiertoDesdeMs = System.currentTimeMillis();
                    break;
                default:
                    break;
            }
        }
    }

    public Estado getEstado(String servicio) {
        Slot s = slots.computeIfAbsent(servicio, k -> new Slot());
        synchronized (s) {
            return s.estado;
        }
    }

    /** Vista inmutable para supervisión (Fase 6). */
    public Map<String, String> snapshot() {
        Map<String, String> out = new HashMap<>();
        for (Map.Entry<String, Slot> e : slots.entrySet()) {
            Slot s = e.getValue();
            synchronized (s) {
                out.put(e.getKey(), s.estado.name());
            }
        }
        return out;
    }
}