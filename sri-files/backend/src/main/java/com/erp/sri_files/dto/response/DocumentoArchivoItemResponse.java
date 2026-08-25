package com.erp.sri_files.dto.response;

public record DocumentoArchivoItemResponse(
        String tipo,
        String nombre,
        String mimeType,
        Long tamanio,
        String fechaCreacion
) {
}
