package com.erp.sri_files.dto.response;

import java.util.List;

public record RideContratoSeccionResponse(
        String nombre,
        List<RideContratoCampoResponse> campos
) {
}
