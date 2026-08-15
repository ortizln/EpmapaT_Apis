package com.erp.sri_files.dto.response;

import java.util.List;

public record DocumentoSeccionContratoResponse(
        String nombre,
        boolean multiple,
        List<DocumentoCampoContratoResponse> campos
) {
}
