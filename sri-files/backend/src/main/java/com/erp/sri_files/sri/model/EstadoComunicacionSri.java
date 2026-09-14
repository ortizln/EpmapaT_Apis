package com.erp.sri_files.sri.model;

/**
 * Estado técnico de la comunicación con el SRI.
 * Complementa el estado funcional del documento: un documento puede estar
 * PENDIENTE_AUTORIZACION mientras el estado de comunicación es CONNECTION_RESET.
 */
public enum EstadoComunicacionSri {
    PENDIENTE,
    OK,
    TIMEOUT,
    CONNECTION_RESET,
    HTTP_ERROR,
    SOAP_ERROR,
    SRI_NO_DISPONIBLE,
    RESULTADO_INCIERTO
}