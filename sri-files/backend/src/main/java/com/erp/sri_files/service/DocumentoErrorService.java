package com.erp.sri_files.service;

import com.erp.sri_files.domain.documento.DocumentoElectronico;
import com.erp.sri_files.domain.documento.DocumentoError;
import com.erp.sri_files.domain.documento.DocumentoEtapa;
import com.erp.sri_files.repositories.documento.DocumentoErrorRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.time.LocalDateTime;

@Service
public class DocumentoErrorService {

    private final DocumentoErrorRepository documentoErrorRepository;

    public DocumentoErrorService(DocumentoErrorRepository documentoErrorRepository) {
        this.documentoErrorRepository = documentoErrorRepository;
    }

    @Transactional
    public void registrar(
            DocumentoElectronico documento,
            DocumentoEtapa etapa,
            String codigo,
            String mensaje,
            Exception exception,
            boolean recuperable
    ) {
        DocumentoError error = new DocumentoError();
        error.setDocumento(documento);
        error.setEtapa(etapa);
        error.setCodigo(codigo);
        error.setMensaje(mensaje);
        error.setDetalle(exception == null ? null : exception.getMessage());
        error.setStackTrace(exception == null ? null : stackTrace(exception));
        error.setRecuperable(recuperable);
        error.setResuelto(false);
        error.setCreatedAt(LocalDateTime.now());
        documentoErrorRepository.save(error);
    }

    private String stackTrace(Exception exception) {
        StringWriter sw = new StringWriter();
        exception.printStackTrace(new PrintWriter(sw));
        return sw.toString();
    }
}
