package com.erp.sri_files.dto.response;

import java.util.List;

public record RideContratoDocumentoResponse(
        String documentoId,
        String empresaId,
        String tipoDocumento,
        String plantillaPredeterminadaId,
        List<RideContratoCampoResponse> parametros,
        RideContratoSeccionResponse detail,
        List<RideContratoCampoResponse> recursos
) {
}
