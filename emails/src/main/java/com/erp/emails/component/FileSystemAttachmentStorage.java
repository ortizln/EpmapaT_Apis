package com.erp.emails.component;

import com.erp.emails.interfaces.AttachmentStorage;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.UUID;

@Component
public class FileSystemAttachmentStorage implements AttachmentStorage {

    private final Path basePath;

    public FileSystemAttachmentStorage(
            @Value("${mailms.attachment-storage.base-path:${xml.storage.path:.//Files//mail-attachments//}}")
            String basePath) {
        this.basePath = Paths.get(basePath).toAbsolutePath().normalize();
    }

    @Override
    public StoredObject save(String filename, String contentType, byte[] bytes) {
        try {
            Files.createDirectories(basePath);
            String safeName = filename.replaceAll("[^a-zA-Z0-9._-]", "_");
            String key = UUID.randomUUID() + "_" + safeName;
            Path out = basePath.resolve(key);
            Files.write(out, bytes, StandardOpenOption.CREATE_NEW);

            String sha = sha256(bytes);
            return new StoredObject(out.toString(), bytes.length, sha);
        } catch (IOException e) {
            throw new RuntimeException("No se pudo guardar adjunto en disco", e);
        }
    }

    @Override
    public void delete(String storageRef) {
        if (storageRef == null || storageRef.isBlank()) {
            return;
        }
        if (storageRef.startsWith("db://")) {
            return;
        }
        try {
            Files.deleteIfExists(Paths.get(storageRef));
        } catch (IOException | InvalidPathException ignored) {
        }
    }

    private String sha256(byte[] bytes) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(md.digest(bytes));
        } catch (Exception e) {
            return null;
        }
    }
}
