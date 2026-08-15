package com.erp.sri_files.storage;

public interface StorageService {

    String buildDocumentoPath(String tipoDocumento, String claveAcceso, String identificador);

    String buildArchivoPath(String tipoDocumento, String claveAcceso, String identificador, String nombreArchivo);

    void saveText(String path, String content);

    void saveBytes(String path, byte[] content);
}
