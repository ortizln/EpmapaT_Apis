package com.erp.sri_files.storage;

import org.springframework.stereotype.Component;

@Component
public class StoragePathResolver {

    private final StorageService storageService;

    public StoragePathResolver(StorageService storageService) {
        this.storageService = storageService;
    }

    public String resolveDocumentoRoot(String tipoDocumento, String claveAcceso, String identificador) {
        return storageService.buildDocumentoPath(tipoDocumento, claveAcceso, identificador);
    }

    public String resolveArchivo(String tipoDocumento, String claveAcceso, String identificador, String nombreArchivo) {
        return storageService.buildArchivoPath(tipoDocumento, claveAcceso, identificador, nombreArchivo);
    }

    public void saveText(String path, String content) {
        storageService.saveText(path, content);
    }

    public void saveBytes(String path, byte[] content) {
        storageService.saveBytes(path, content);
    }
}
