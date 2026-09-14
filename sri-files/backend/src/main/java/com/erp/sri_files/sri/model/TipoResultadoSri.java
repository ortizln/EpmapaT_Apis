package com.erp.sri_files.sri.model;

/**
 * Clasificación de resultado de una comunicación con el SRI.
 * Separa el estado funcional del documento del resultado técnico de la comunicación.
 */
public enum TipoResultadoSri {

    /** El comprobante fue recibido por el SRI (estado RECIBIDA). */
    RECIBIDA,

    /** El comprobante fue devuelto por validación funcional del SRI. No es reintentable. */
    DEVUELTA,

    /** Autorización SRI confirmada con XML autorizado recuperado. */
    AUTORIZADA,

    /** El SRI respondió con estado NO AUTORIZADO. No es reintentable. */
    NO_AUTORIZADA,

    /** Código 43: CLAVE ACCESO REGISTRADA. No reenviar a Recepción: consultar Autorización. */
    CLAVE_REGISTRADA,

    /** Error de infraestructura/red recuperable (timeout, connection reset, 502/503/504). */
    ERROR_TRANSITORIO,

    /** Error funcional definitivo (XML inválido, firma inválida, esquema, datos tributarios). */
    ERROR_NO_RECUPERABLE,

    /** No hubo respuesta util (respuesta nula, comprobante inexistente, pendiente). */
    SIN_RESPUESTA
}