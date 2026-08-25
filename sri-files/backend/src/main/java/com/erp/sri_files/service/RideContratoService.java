package com.erp.sri_files.service;

import com.erp.sri_files.domain.documento.DocumentoArchivoTipo;
import com.erp.sri_files.domain.documento.DocumentoElectronico;
import com.erp.sri_files.dto.response.RideContratoDocumentoResponse;
import com.erp.sri_files.exceptions.DocumentoRecepcionException;
import com.erp.sri_files.repositories.documento.DocumentoElectronicoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

@Service
public class RideContratoService {

    private final DocumentoElectronicoRepository documentoElectronicoRepository;
    private final ArchivoDocumentoService archivoDocumentoService;
    private final JasperRideTemplateRenderer jasperRideTemplateRenderer;

    public RideContratoService(
            DocumentoElectronicoRepository documentoElectronicoRepository,
            ArchivoDocumentoService archivoDocumentoService,
            JasperRideTemplateRenderer jasperRideTemplateRenderer
    ) {
        this.documentoElectronicoRepository = documentoElectronicoRepository;
        this.archivoDocumentoService = archivoDocumentoService;
        this.jasperRideTemplateRenderer = jasperRideTemplateRenderer;
    }

    @Transactional(readOnly = true)
    public RideContratoDocumentoResponse obtener(UUID documentoUuid) {
        DocumentoElectronico documento = documentoElectronicoRepository.findByUuid(documentoUuid)
                .orElseThrow(() -> new DocumentoRecepcionException("No existe documento con uuid " + documentoUuid));
        byte[] xmlAutorizado = archivoDocumentoService.leer(documentoUuid, DocumentoArchivoTipo.XML_AUTORIZADO);
        return jasperRideTemplateRenderer.construirContrato(
                documento,
                new String(xmlAutorizado, StandardCharsets.UTF_8)
        );
    }
}
