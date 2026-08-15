package com.erp.sri_files.dto.response;

import java.util.List;

public record DocumentoCorreoSeguimientoResponse(
        String id,
        String destinatario,
        String remitente,
        String estadoActual,
        boolean requiereIntervencion,
        boolean correoConfigurado,
        List<DocumentoCorreoEventoResponse> eventos
) {
}
