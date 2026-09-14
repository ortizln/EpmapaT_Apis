package com.erp.sri_files.sri.retry;

/**
 * Acción que el motor de reintentos debe aplicar a un documento
 * según su bitácora de intentos (sri_intento_comunicacion).
 */
public enum SriReintentoDecision {

    /** No reintentar: el documento ya no es reintentable (cerrado). */
    IGNORAR,

    /** Aún no ha llegado el momento programado (fecha_proximo_intento en el futuro). */
    AUN_NO,

    /** El documento agotó los intentos máximos: debe cerrarse para revisión manual. */
    AGOTADO,

    /** Le toca reintentar ahora: consultar la autorización. */
    CONSULTAR
}