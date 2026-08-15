package com.erp.sri_files.dto.response;

import java.util.List;

public record DocumentoIntentoSriResponse(
        String id,
        int totalIntentos,
        String estadoActual,
        boolean requiereIntervencion,
        String fechaInicioProcesamiento,
        String fechaFinalizacion,
        List<DocumentoIntentoSriItemResponse> eventos
) {
}
