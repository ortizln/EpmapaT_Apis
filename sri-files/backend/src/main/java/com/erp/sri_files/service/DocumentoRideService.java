package com.erp.sri_files.service;

import com.erp.sri_files.domain.documento.DocumentoElectronico;
import com.erp.sri_files.domain.documento.PlantillaRide;
import com.erp.sri_files.domain.documento.TipoDocumento;
import com.erp.sri_files.repositories.documento.PlantillaRideRepository;
import com.erp.sri_files.retenciones.service.RetencionPdfService;
import com.erp.sri_files.ride.RideService;
import org.springframework.stereotype.Service;

import java.nio.file.Path;

@Service
public class DocumentoRideService {

    private final RideService rideService;
    private final RetencionPdfService retencionPdfService;
    private final BasicPdfDocumentService basicPdfDocumentService;
    private final PlantillaRideRepository plantillaRideRepository;
    private final JasperRideTemplateRenderer jasperRideTemplateRenderer;

    public DocumentoRideService(
            RideService rideService,
            RetencionPdfService retencionPdfService,
            BasicPdfDocumentService basicPdfDocumentService,
            PlantillaRideRepository plantillaRideRepository,
            JasperRideTemplateRenderer jasperRideTemplateRenderer
    ) {
        this.rideService = rideService;
        this.retencionPdfService = retencionPdfService;
        this.basicPdfDocumentService = basicPdfDocumentService;
        this.plantillaRideRepository = plantillaRideRepository;
        this.jasperRideTemplateRenderer = jasperRideTemplateRenderer;
    }

    public byte[] generar(DocumentoElectronico documento, String xmlAutorizado) throws Exception {
        PlantillaRide plantilla = documento.getEmpresa() == null ? null : plantillaRideRepository
                .findFirstByEmpresaAndTipoDocumentoAndPredeterminadaTrueAndActivaTrue(documento.getEmpresa(), documento.getTipoDocumento())
                .orElse(null);
        if (plantilla != null && plantilla.getRutaArchivo() != null && !plantilla.getRutaArchivo().isBlank()) {
            return jasperRideTemplateRenderer.render(
                    xmlAutorizado,
                    documento.getTipoDocumento(),
                    Path.of(plantilla.getRutaArchivo()),
                    documento.getEmpresa()
            );
        }
        if (documento.getTipoDocumento() == TipoDocumento.RETENCION) {
            return retencionPdfService.generarPdfDesdeXmlAutorizado(xmlAutorizado);
        }
        if (documento.getTipoDocumento() == TipoDocumento.LIQUIDACION_COMPRA) {
            return basicPdfDocumentService.generarDesdeXml("Liquidacion de compra autorizada", xmlAutorizado);
        }
        if (documento.getTipoDocumento() == TipoDocumento.NOTA_CREDITO) {
            return basicPdfDocumentService.generarNotaCreditoDesdeXml(xmlAutorizado);
        }
        if (documento.getTipoDocumento() == TipoDocumento.NOTA_DEBITO) {
            return basicPdfDocumentService.generarNotaDebitoDesdeXml(xmlAutorizado);
        }
        if (documento.getTipoDocumento() == TipoDocumento.GUIA_REMISION) {
            return basicPdfDocumentService.generarGuiaRemisionDesdeXml(xmlAutorizado);
        }
        return rideService.generar(xmlAutorizado);
    }
}
