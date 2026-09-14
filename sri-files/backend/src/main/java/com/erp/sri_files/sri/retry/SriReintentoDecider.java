package com.erp.sri_files.sri.retry;

import java.time.LocalDateTime;

/**
 * Decide qué acción aplica el motor de reintentos a un documento.
 * Lógica pura (sin BD ni Spring) para poder probarse en unit tests.
 *
 * Regla:
 * <ul>
 *   <li>No reintentable -&gt; IGNORAR</li>
 *   <li>numeroIntento &gt;= maxIntentos -&gt; AGOTADO (cierre manual)</li>
 *   <li>fecha_proximo_intento aun no llega -&gt; AUN_NO</li>
 *   <li>Resto -&gt; CONSULTAR (reintentar ahora)</li>
 * </ul>
 */
public final class SriReintentoDecider {

    private SriReintentoDecider() {
    }

    public static SriReintentoDecision decidir(
            boolean reintentable,
            int numeroIntento,
            int maxIntentos,
            LocalDateTime fechaProximoIntento,
            LocalDateTime ahora) {
        if (!reintentable) {
            return SriReintentoDecision.IGNORAR;
        }
        if (numeroIntento >= maxIntentos) {
            return SriReintentoDecision.AGOTADO;
        }
        if (fechaProximoIntento == null || ahora == null || ahora.isBefore(fechaProximoIntento)) {
            return SriReintentoDecision.AUN_NO;
        }
        return SriReintentoDecision.CONSULTAR;
    }
}