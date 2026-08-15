package com.erp.sri_files.storage;

import com.erp.sri_files.config.SriFilesProperties;
import com.erp.sri_files.exceptions.DocumentoRecepcionException;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;

@Service
public class LocalStorageService implements StorageService {

    private final SriFilesProperties properties;

    public LocalStorageService(SriFilesProperties properties) {
        this.properties = properties;
    }

    @Override
    public String buildDocumentoPath(String tipoDocumento, String claveAcceso, String identificador) {
        LocalDate today = LocalDate.now();
        String folderName = claveAcceso == null || claveAcceso.isBlank() ? sanitizeSegment(identificador) : claveAcceso;
        return Path.of(
                properties.getStorage().getRoot(),
                String.valueOf(today.getYear()),
                String.format("%02d", today.getMonthValue()),
                sanitizeSegment(tipoDocumento),
                folderName == null || folderName.isBlank() ? "sin-identificador" : folderName
        ).toString();
    }

    @Override
    public String buildArchivoPath(String tipoDocumento, String claveAcceso, String identificador, String nombreArchivo) {
        return Path.of(
                buildDocumentoPath(tipoDocumento, claveAcceso, identificador),
                sanitizeSegment(nombreArchivo)
        ).toString();
    }

    @Override
    public void saveText(String path, String content) {
        saveBytes(path, content == null ? new byte[0] : content.getBytes(StandardCharsets.UTF_8));
    }

    @Override
    public void saveBytes(String path, byte[] content) {
        try {
            Path target = Path.of(path);
            Files.createDirectories(target.getParent());
            Files.write(target, content == null ? new byte[0] : content);
        } catch (IOException ex) {
            throw new DocumentoRecepcionException("No se pudo guardar el archivo del documento en " + path);
        }
    }

    private String sanitizeSegment(String value) {
        if (value == null) {
            return null;
        }
        String sanitized = value.trim().toLowerCase()
                .replace("\\", "-")
                .replace("/", "-")
                .replace(":", "-")
                .replace(" ", "_");
        return sanitized.isBlank() ? null : sanitized;
    }
}
