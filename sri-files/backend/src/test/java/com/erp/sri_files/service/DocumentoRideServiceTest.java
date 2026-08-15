package com.erp.sri_files.service;

import com.erp.sri_files.domain.documento.DocumentoElectronico;
import com.erp.sri_files.domain.documento.TipoDocumento;
import com.erp.sri_files.retenciones.service.RetencionPdfService;
import com.erp.sri_files.ride.RideService;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DocumentoRideServiceTest {

    @Test
    void usaPdfEspecializadoParaGuiaRemision() throws Exception {
        RideService rideService = mock(RideService.class);
        RetencionPdfService retencionPdfService = mock(RetencionPdfService.class);
        BasicPdfDocumentService basicPdfDocumentService = mock(BasicPdfDocumentService.class);
        DocumentoRideService service = new DocumentoRideService(rideService, retencionPdfService, basicPdfDocumentService);

        DocumentoElectronico documento = new DocumentoElectronico();
        documento.setTipoDocumento(TipoDocumento.GUIA_REMISION);
        byte[] expected = new byte[]{9, 8, 7};
        when(basicPdfDocumentService.generarGuiaRemisionDesdeXml("<xml/>")).thenReturn(expected);

        byte[] result = service.generar(documento, "<xml/>");

        assertArrayEquals(expected, result);
        verify(basicPdfDocumentService).generarGuiaRemisionDesdeXml("<xml/>");
    }

    @Test
    void usaPdfEspecializadoParaNotaCredito() throws Exception {
        RideService rideService = mock(RideService.class);
        RetencionPdfService retencionPdfService = mock(RetencionPdfService.class);
        BasicPdfDocumentService basicPdfDocumentService = mock(BasicPdfDocumentService.class);
        DocumentoRideService service = new DocumentoRideService(rideService, retencionPdfService, basicPdfDocumentService);

        DocumentoElectronico documento = new DocumentoElectronico();
        documento.setTipoDocumento(TipoDocumento.NOTA_CREDITO);
        byte[] expected = new byte[]{4, 5, 6};
        when(basicPdfDocumentService.generarNotaCreditoDesdeXml("<xml-credito/>")).thenReturn(expected);

        byte[] result = service.generar(documento, "<xml-credito/>");

        assertArrayEquals(expected, result);
        verify(basicPdfDocumentService).generarNotaCreditoDesdeXml("<xml-credito/>");
    }

    @Test
    void usaPdfEspecializadoParaNotaDebito() throws Exception {
        RideService rideService = mock(RideService.class);
        RetencionPdfService retencionPdfService = mock(RetencionPdfService.class);
        BasicPdfDocumentService basicPdfDocumentService = mock(BasicPdfDocumentService.class);
        DocumentoRideService service = new DocumentoRideService(rideService, retencionPdfService, basicPdfDocumentService);

        DocumentoElectronico documento = new DocumentoElectronico();
        documento.setTipoDocumento(TipoDocumento.NOTA_DEBITO);
        byte[] expected = new byte[]{1, 3, 5};
        when(basicPdfDocumentService.generarNotaDebitoDesdeXml("<xml-debito/>")).thenReturn(expected);

        byte[] result = service.generar(documento, "<xml-debito/>");

        assertArrayEquals(expected, result);
        verify(basicPdfDocumentService).generarNotaDebitoDesdeXml("<xml-debito/>");
    }
}
