package com.erp.sri_files.service;

import com.erp.sri_files.domain.documento.DocumentoArchivo;
import com.erp.sri_files.domain.documento.DocumentoElectronico;
import com.erp.sri_files.domain.documento.DocumentoArchivoTipo;
import com.erp.sri_files.dto.response.DocumentoArchivoItemResponse;
import com.erp.sri_files.exceptions.DocumentoRecepcionException;
import com.erp.sri_files.repositories.documento.DocumentoArchivoRepository;
import com.erp.sri_files.storage.StoragePathResolver;
import org.springframework.stereotype.Service;

import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.List;
import java.util.UUID;

@Service
public class ArchivoDocumentoService {

    private final StoragePathResolver storagePathResolver;
    private final DocumentoArchivoRepository documentoArchivoRepository;

    public ArchivoDocumentoService(
            StoragePathResolver storagePathResolver,
            DocumentoArchivoRepository documentoArchivoRepository
    ) {
        this.storagePathResolver = storagePathResolver;
        this.documentoArchivoRepository = documentoArchivoRepository;
    }

    public String resolverRutaDocumento(DocumentoElectronico documento) {
        return storagePathResolver.resolveDocumentoRoot(
                documento.getTipoDocumento().name(),
                documento.getClaveAcceso(),
                documento.getUuid().toString()
        );
    }

    public String resolverRutaArchivo(DocumentoElectronico documento, DocumentoArchivoTipo tipoArchivo) {
        return storagePathResolver.resolveArchivo(
                documento.getTipoDocumento().name(),
                documento.getClaveAcceso(),
                documento.getUuid().toString(),
                nombreArchivo(tipoArchivo)
        );
    }

    public String guardarJsonOriginal(DocumentoElectronico documento) {
        return guardarTexto(documento, DocumentoArchivoTipo.JSON_ORIGINAL, documento.getJsonOriginal());
    }

    public String guardarTexto(DocumentoElectronico documento, DocumentoArchivoTipo tipoArchivo, String contenido) {
        String path = resolverRutaArchivo(documento, tipoArchivo);
        storagePathResolver.saveText(path, contenido);
        registrarArchivo(documento, tipoArchivo, path, mimeType(tipoArchivo));
        return path;
    }

    public String guardarBytes(DocumentoElectronico documento, DocumentoArchivoTipo tipoArchivo, byte[] contenido) {
        String path = resolverRutaArchivo(documento, tipoArchivo);
        storagePathResolver.saveBytes(path, contenido);
        registrarArchivo(documento, tipoArchivo, path, mimeType(tipoArchivo));
        return path;
    }

    public List<DocumentoArchivoItemResponse> listar(UUID documentoUuid) {
        return documentoArchivoRepository.findByDocumento_UuidAndActivoTrueOrderByCreatedAtDesc(documentoUuid).stream()
                .map(item -> new DocumentoArchivoItemResponse(
                        item.getTipoArchivo().name(),
                        item.getNombreArchivo(),
                        item.getMimeType(),
                        item.getTamanio(),
                        item.getCreatedAt() != null ? item.getCreatedAt().toString() : null
                ))
                .toList();
    }

    public byte[] leer(UUID documentoUuid, DocumentoArchivoTipo tipoArchivo) {
        DocumentoArchivo archivo = documentoArchivoRepository
                .findFirstByDocumento_UuidAndTipoArchivoAndActivoTrueOrderByCreatedAtDesc(documentoUuid, tipoArchivo)
                .orElseThrow(() -> new DocumentoRecepcionException(
                        "No existe archivo " + tipoArchivo.name() + " para el documento " + documentoUuid
                ));
        try {
            return Files.readAllBytes(Path.of(archivo.getRuta()));
        } catch (Exception ex) {
            throw new DocumentoRecepcionException("No fue posible leer el archivo " + archivo.getNombreArchivo());
        }
    }

    public String nombreDescarga(UUID documentoUuid, DocumentoArchivoTipo tipoArchivo) {
        return documentoArchivoRepository
                .findFirstByDocumento_UuidAndTipoArchivoAndActivoTrueOrderByCreatedAtDesc(documentoUuid, tipoArchivo)
                .map(DocumentoArchivo::getNombreArchivo)
                .orElse(tipoArchivo.name().toLowerCase());
    }

    public String mimeType(UUID documentoUuid, DocumentoArchivoTipo tipoArchivo) {
        return documentoArchivoRepository
                .findFirstByDocumento_UuidAndTipoArchivoAndActivoTrueOrderByCreatedAtDesc(documentoUuid, tipoArchivo)
                .map(DocumentoArchivo::getMimeType)
                .orElse(mimeType(tipoArchivo));
    }

    private String nombreArchivo(DocumentoArchivoTipo tipoArchivo) {
        return switch (tipoArchivo) {
            case JSON_ORIGINAL -> "json_original.json";
            case XML_GENERADO -> "xml_generado.xml";
            case XML_FIRMADO -> "xml_firmado.xml";
            case XML_AUTORIZADO -> "xml_autorizado.xml";
            case RIDE_PDF -> "ride.pdf";
            case RESPUESTA_SRI -> "respuesta_sri.json";
        };
    }

    private String mimeType(DocumentoArchivoTipo tipoArchivo) {
        return switch (tipoArchivo) {
            case JSON_ORIGINAL, RESPUESTA_SRI -> "application/json";
            case XML_GENERADO, XML_FIRMADO, XML_AUTORIZADO -> "application/xml";
            case RIDE_PDF -> "application/pdf";
        };
    }

    private void registrarArchivo(DocumentoElectronico documento, DocumentoArchivoTipo tipoArchivo, String path, String mimeType) {
        try {
            Path archivoPath = Path.of(path);
            byte[] bytes = Files.readAllBytes(archivoPath);

            DocumentoArchivo archivo = new DocumentoArchivo();
            archivo.setDocumento(documento);
            archivo.setTipoArchivo(tipoArchivo);
            archivo.setNombreArchivo(nombreArchivo(tipoArchivo));
            archivo.setMimeType(mimeType);
            archivo.setRuta(path);
            archivo.setTamanio((long) bytes.length);
            archivo.setHashSha256(HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes)));
            archivo.setVersion(1);
            archivo.setActivo(true);
            archivo.setCreatedAt(LocalDateTime.now());
            documentoArchivoRepository.save(archivo);
        } catch (Exception ex) {
            throw new DocumentoRecepcionException("No fue posible registrar el archivo " + tipoArchivo.name());
        }
    }
}
