package com.erp.sri_files.sri.retry;

import com.erp.sri_files.sri.model.ResultadoSri;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Política centralizada de reintentos de comunicación con el SRI.
 *
 * Equivalencia por defecto (1 min, 5 min, 15 min, 30 min, 60 min):
 * <pre>
 *   Intento 1 -> inicial
 *   Intento 2 -> +1 minuto
 *   Intento 3 -> +5 minutos
 *   Intento 4 -> +15 minutos
 *   Intento 5 -> +30 minutos
 *   Intento 6 -> +60 minutos
 * </pre>
 */
@Component
public class SriRetryPolicy {

    private final boolean enabled;
    private final int maxAttempts;
    private final List<Long> delaysSeconds;

    public SriRetryPolicy(
            @Value("${sri.retry.enabled:false}") boolean enabled,
            @Value("${sri.retry.max-attempts:6}") int maxAttempts,
            @Value("${sri.retry.delays-seconds:60,300,900,1800,3600}") String delaysSecondsCsv) {
        this.enabled = enabled;
        this.maxAttempts = Math.max(1, maxAttempts);
        this.delaysSeconds = parseDelays(delaysSecondsCsv);
    }

    public boolean isEnabled() {
        return enabled;
    }

    public int getMaxAttempts() {
        return maxAttempts;
    }

    public List<Long> getDelaysSeconds() {
        return delaysSeconds;
    }

    /** True si el resultado permite reintento controlado y aún no se superó el máximo. */
    public boolean debeReintentar(ResultadoSri resultado, int intentoActual) {
        if (resultado == null || !resultado.isReintentable()) return false;
        if (!enabled) return false;
        return intentoActual < maxAttempts;
    }

    /** Espera en segundos que debe aplicarse después de un intento fallido (antes del siguiente). */
    public long delaySegundosParaIntento(int intentoFallido) {
        if (delaysSeconds.isEmpty()) return 60L;
        int idx = intentoFallido - 1;
        if (idx >= delaysSeconds.size()) {
            // crecer con el último retardo escalado por el excedente
            return (delaysSeconds.get(delaysSeconds.size() - 1)) * (idx - delaysSeconds.size() + 2);
        }
        return delaysSeconds.get(Math.max(0, idx));
    }

    /** Próximo momento permitido para reintentar, programado por el número de intento fallido. */
    public LocalDateTime proximoIntento(LocalDateTime ahora, int intentoFallido) {
        return ahora.plusSeconds(delaySegundosParaIntento(intentoFallido));
    }

    private static List<Long> parseDelays(String csv) {
        if (csv == null || csv.isBlank()) return List.of(60L);
        return Arrays.stream(csv.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .map(Long::parseLong)
                .collect(Collectors.toCollection(ArrayList::new));
    }
}