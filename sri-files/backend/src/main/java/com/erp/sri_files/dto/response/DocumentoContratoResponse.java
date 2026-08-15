package com.erp.sri_files.dto.response;

import java.util.List;

public record DocumentoContratoResponse(
        String tipoDocumento,
        String endpoint,
        String metodo,
        List<DocumentoSeccionContratoResponse> secciones
) {
}
