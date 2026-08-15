package com.erp.sri_files.service;

import com.erp.sri_files.domain.documento.DocumentoElectronico;
import com.erp.sri_files.domain.documento.DocumentoEstado;
import com.erp.sri_files.domain.documento.DocumentoEtapa;
import com.erp.sri_files.processor.DocumentoProcessor;
import com.erp.sri_files.processor.DocumentoProcessorFactory;
import com.erp.sri_files.repositories.documento.DocumentoElectronicoRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class DocumentoProcessingService {

    private static final Logger log = LoggerFactory.getLogger(DocumentoProcessingService.class);

    private final DocumentoElectronicoRepository documentoRepository;
    private final EstadoDocumentoService estadoDocumentoService;
    private final DocumentoProcessorFactory documentoProcessorFactory;
    private final DocumentoErrorService documentoErrorService;
    private final DocumentoWorkflowService documentoWorkflowService;

    public DocumentoProcessingService(
            DocumentoElectronicoRepository documentoRepository,
            EstadoDocumentoService estadoDocumentoService,
            DocumentoProcessorFactory documentoProcessorFactory,
            DocumentoErrorService documentoErrorService,
            DocumentoWorkflowService documentoWorkflowService
    ) {
        this.documentoRepository = documentoRepository;
        this.estadoDocumentoService = estadoDocumentoService;
        this.documentoProcessorFactory = documentoProcessorFactory;
        this.documentoErrorService = documentoErrorService;
        this.documentoWorkflowService = documentoWorkflowService;
    }

    @Transactional
    public int procesarPendientesRecibidos() {
        List<DocumentoElectronico> pendientes = documentoRepository.findTop20ByEstadoActualOrderByFechaRecepcionAsc(DocumentoEstado.RECIBIDO);
        int procesados = 0;

        for (DocumentoElectronico documento : pendientes) {
            try {
                estadoDocumentoService.cambiar(documento, DocumentoEstado.VALIDANDO, "Documento reclamado por worker");
                DocumentoProcessor processor = documentoProcessorFactory.obtener(documento.getTipoDocumento());
                processor.validar(documento);
                estadoDocumentoService.cambiar(documento, DocumentoEstado.VALIDADO, "Validacion base completada");
                documentoWorkflowService.procesar(documento);
                procesados++;
            } catch (Exception ex) {
                log.error("Error procesando documento uuid={}", documento.getUuid(), ex);
                if (documento.getEstadoActual() == DocumentoEstado.VALIDANDO) {
                    documentoErrorService.registrar(
                            documento,
                            DocumentoEtapa.VALIDACION,
                            "DOC_VALIDATION_ERROR",
                            "Error en validacion base",
                            ex,
                            false
                    );
                    estadoDocumentoService.cambiar(documento, DocumentoEstado.ERROR_VALIDACION, "Error en validacion base: " + ex.getMessage());
                }
            }
        }

        return procesados;
    }
}
