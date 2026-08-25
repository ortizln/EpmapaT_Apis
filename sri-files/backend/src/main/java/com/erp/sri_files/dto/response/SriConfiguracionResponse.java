package com.erp.sri_files.dto.response;

public record SriConfiguracionResponse(
        String empresaId,
        int ambiente,
        int timeoutConexionMs,
        int timeoutRespuestaMs,
        int maxReintentos,
        boolean activo
) {
}
