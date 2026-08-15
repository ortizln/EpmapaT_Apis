package com.erp.sri_files.dto.response;

import java.util.List;

public record DocumentoResumenOperativoResponse(
        long totalDocumentos,
        long recibidosHoy,
        long autorizados,
        long pendientes,
        long conErrores,
        long requiereIntervencion,
        List<DocumentoConteoResponse> porTipo,
        List<DocumentoConteoResponse> porEstado
) {
}
