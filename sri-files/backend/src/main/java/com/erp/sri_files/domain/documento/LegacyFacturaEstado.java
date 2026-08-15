package com.erp.sri_files.domain.documento;

import java.util.Arrays;

public enum LegacyFacturaEstado {
    I("Pendiente de procesamiento o reintento", DocumentoEstado.RECIBIDO),
    P("Procesando en flujo interno", DocumentoEstado.VALIDANDO),
    C("Pendiente de autorizacion SRI", DocumentoEstado.PENDIENTE_AUTORIZACION),
    A("Autorizada", DocumentoEstado.AUTORIZADO),
    O("Autorizada con correo pendiente o fallido", DocumentoEstado.CORREO_PENDIENTE),
    N("No autorizada", DocumentoEstado.NO_AUTORIZADO),
    M("Devuelta u observada por SRI", DocumentoEstado.DEVUELTO_SRI);

    private final String descripcion;
    private final DocumentoEstado estadoObjetivo;

    LegacyFacturaEstado(String descripcion, DocumentoEstado estadoObjetivo) {
        this.descripcion = descripcion;
        this.estadoObjetivo = estadoObjetivo;
    }

    public String getCodigo() {
        return name();
    }

    public String getDescripcion() {
        return descripcion;
    }

    public DocumentoEstado getEstadoObjetivo() {
        return estadoObjetivo;
    }

    public static LegacyFacturaEstado fromCodigo(String codigo) {
        return Arrays.stream(values())
                .filter(value -> value.name().equalsIgnoreCase(codigo == null ? "" : codigo.trim()))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Estado legacy no soportado: " + codigo));
    }
}
