package com.erp.sri_files.sri.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Resultado clasificado de una comunicación con el SRI.
 * El código de negocio decide el siguiente paso a partir de estos campos y
 * nunca debe depender de comparar textos dispersos como
 * mensaje.contains("CLAVE ACCESO REGISTRADA").
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ResultadoSri {

    /** Clasificación funcional del resultado. */
    private TipoResultadoSri tipo;

    /** Estado técnico de la comunicación. */
    private EstadoComunicacionSri estadoComunicacion;

    /** Código SRI (p.ej. 43) o directamente "43" para CLAVE ACCESO REGISTRADA. */
    private String codigo;

    /** Mensaje devuelto por el SRI o mensaje de la excepción. */
    private String mensaje;

    /** Información adicional devuelta por el SRI. */
    private String informacionAdicional;

    /** True si el resultado admite reintento de comunicación controlado. */
    private boolean reintentable;

    /** True si el siguiente paso debe ser consultar la autorización por clave de acceso. */
    private boolean requiereConsultaAutorizacion;

    /** Causa raíz preservada de la excepción técnica (sin stack trace completo). */
    private String excepcion;

    /** Duración de la llamada en ms. */
    private long duracionMs;

    /** Número de intento (1 = inicial). */
    private int numeroIntento;

    /** Servicio SRI consultado. */
    private String servicio;

    /** Ambiente: 1 = PRUEBAS, 2 = PRODUCCIÓN. */
    private String ambiente;

    /** Número de autorización cuando el resultado es AUTORIZADA. */
    private String numeroAutorizacion;

    /** Fecha/hora de autorización cuando el resultado es AUTORIZADA. */
    private LocalDateTime fechaAutorizacion;

    /** XML autorizado completo recuperado del SRI. */
    private String xmlAutorizado;

    /** Clave de acceso asociada a la comunicación. */
    private String claveAcceso;

    public boolean esAutorizada() {
        return tipo == TipoResultadoSri.AUTORIZADA;
    }

    public boolean esResultadoIncierto() {
        return requiereConsultaAutorizacion && reintentable;
    }
}