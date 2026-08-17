package com.erp.sri_files.dto.response;

import java.util.List;

public record DocumentoAuditoriaResumenResponse(
        long totalEventos,
        List<DocumentoAuditoriaEventoResponse> eventos
) {
}
