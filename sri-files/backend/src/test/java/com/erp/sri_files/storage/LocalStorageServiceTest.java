package com.erp.sri_files.storage;

import com.erp.sri_files.config.SriFilesProperties;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

class LocalStorageServiceTest {

    @Test
    void construyeRutaConEstructuraEsperada() {
        SriFilesProperties properties = new SriFilesProperties();
        properties.getStorage().setRoot("data/sri-files");
        LocalStorageService service = new LocalStorageService(properties);

        String path = service.buildDocumentoPath("FACTURA", "12345", "abc-1");

        assertTrue(path.contains("data"));
        assertTrue(path.contains("factura"));
        assertTrue(path.endsWith("12345"));
    }
}
