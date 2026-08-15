package com.erp.sri_files.service;

import com.erp.sri_files.domain.documento.DocumentoElectronico;
import com.erp.sri_files.domain.documento.TipoDocumento;
import com.erp.sri_files.retenciones.service.RetencionPdfService;
import com.erp.sri_files.ride.RideService;
import org.springframework.stereotype.Service;

@Service
public class DocumentoRideService {

    private final RideService rideService;
    private final RetencionPdfService retencionPdfService;
    private final BasicPdfDocumentService basicPdfDocumentService;

    public DocumentoRideService(
            RideService rideService,
            RetencionPdfService retencionPdfService,
            BasicPdfDocumentService basicPdfDocumentService
    ) {
        this.rideService = rideService;
        this.retencionPdfService = retencionPdfService;
        this.basicPdfDocumentService = basicPdfDocumentService;
    }

    public byte[] generar(DocumentoElectronico documento, String xmlAutorizado) throws Exception {
        if (documento.getTipoDocumento() == TipoDocumento.RETENCION) {
            return retencionPdfService.generarPdfDesdeXmlAutorizado(xmlAutorizado);
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
