package com.erp.sri_files.service;

import com.erp.sri_files.domain.documento.DocumentoArchivoTipo;
import com.erp.sri_files.domain.documento.DocumentoElectronico;
import com.erp.sri_files.domain.documento.PlantillaRide;
import com.erp.sri_files.exceptions.DocumentoRecepcionException;
import com.erp.sri_files.repositories.documento.DocumentoElectronicoRepository;
import com.erp.sri_files.repositories.documento.PlantillaRideRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.UUID;

@Service
public class PlantillaRidePreviewService {

    private final PlantillaRideRepository plantillaRideRepository;
    private final DocumentoElectronicoRepository documentoElectronicoRepository;
    private final ArchivoDocumentoService archivoDocumentoService;
    private final JasperRideTemplateRenderer jasperRideTemplateRenderer;

    public PlantillaRidePreviewService(
            PlantillaRideRepository plantillaRideRepository,
            DocumentoElectronicoRepository documentoElectronicoRepository,
            ArchivoDocumentoService archivoDocumentoService,
            JasperRideTemplateRenderer jasperRideTemplateRenderer
    ) {
        this.plantillaRideRepository = plantillaRideRepository;
        this.documentoElectronicoRepository = documentoElectronicoRepository;
        this.archivoDocumentoService = archivoDocumentoService;
        this.jasperRideTemplateRenderer = jasperRideTemplateRenderer;
    }

    @Transactional(readOnly = true)
    public byte[] generarPreview(UUID plantillaUuid, UUID documentoUuid) {
        PlantillaRide plantilla = plantillaRideRepository.findByUuid(plantillaUuid)
                .orElseThrow(() -> new DocumentoRecepcionException("No existe plantilla RIDE con uuid " + plantillaUuid));
        DocumentoElectronico documento = documentoElectronicoRepository.findByUuid(documentoUuid)
                .orElseThrow(() -> new DocumentoRecepcionException("No existe documento con uuid " + documentoUuid));

        if (plantilla.getRutaArchivo() == null || plantilla.getRutaArchivo().isBlank()) {
            throw new DocumentoRecepcionException("La plantilla no tiene archivo JRXML asociado");
        }
        if (documento.getEmpresa() == null || plantilla.getEmpresa() == null
                || !documento.getEmpresa().getId().equals(plantilla.getEmpresa().getId())) {
            throw new DocumentoRecepcionException("La plantilla y el documento deben pertenecer a la misma empresa");
        }
        if (documento.getTipoDocumento() != plantilla.getTipoDocumento()) {
            throw new DocumentoRecepcionException("El tipo de documento no coincide con la plantilla seleccionada");
        }

        byte[] xmlAutorizado = archivoDocumentoService.leer(documentoUuid, DocumentoArchivoTipo.XML_AUTORIZADO);
        return jasperRideTemplateRenderer.render(
                new String(xmlAutorizado, StandardCharsets.UTF_8),
                documento.getTipoDocumento(),
                Path.of(plantilla.getRutaArchivo()),
                documento.getEmpresa()
        );
    }
}
