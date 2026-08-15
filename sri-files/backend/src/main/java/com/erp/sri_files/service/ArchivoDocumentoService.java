package com.erp.sri_files.service;

import com.erp.sri_files.domain.documento.DocumentoArchivoTipo;
import com.erp.sri_files.domain.documento.DocumentoElectronico;
import com.erp.sri_files.domain.documento.DocumentoArchivoTipo;
import com.erp.sri_files.storage.StoragePathResolver;
import org.springframework.stereotype.Service;

@Service
public class ArchivoDocumentoService {

    private final StoragePathResolver storagePathResolver;

    public ArchivoDocumentoService(StoragePathResolver storagePathResolver) {
        this.storagePathResolver = storagePathResolver;
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
        return path;
    }

    public String guardarBytes(DocumentoElectronico documento, DocumentoArchivoTipo tipoArchivo, byte[] contenido) {
        String path = resolverRutaArchivo(documento, tipoArchivo);
        storagePathResolver.saveBytes(path, contenido);
        return path;
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
}
