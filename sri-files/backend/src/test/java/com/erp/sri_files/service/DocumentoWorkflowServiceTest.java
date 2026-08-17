package com.erp.sri_files.service;

import com.erp.sri_files.domain.documento.DocumentoArchivoTipo;
import com.erp.sri_files.domain.documento.DocumentoElectronico;
import com.erp.sri_files.domain.documento.DocumentoEstado;
import com.erp.sri_files.domain.documento.Empresa;
import com.erp.sri_files.domain.documento.TipoDocumento;
import com.erp.sri_files.mail.CorreoDocumentoService;
import com.erp.sri_files.signature.FirmaElectronicaService;
import com.erp.sri_files.sri.port.SriAutorizacionPort;
import com.erp.sri_files.sri.port.SriRecepcionPort;
import com.fasterxml.jackson.databind.ObjectMapper;
import ec.gob.sri.ws.autorizacion.Autorizacion;
import ec.gob.sri.ws.autorizacion.RespuestaComprobante;
import ec.gob.sri.ws.autorizacion.RespuestaComprobante.Autorizaciones;
import ec.gob.sri.ws.recepcion.RespuestaSolicitud;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DocumentoWorkflowServiceTest {

    private DocumentoXmlService documentoXmlService;
    private DocumentoXmlValidationService documentoXmlValidationService;
    private ArchivoDocumentoService archivoDocumentoService;
    private FirmaElectronicaService firmaElectronicaService;
    private SriRecepcionPort sriRecepcionPort;
    private SriAutorizacionPort sriAutorizacionPort;
    private DocumentoRideService documentoRideService;
    private CorreoDocumentoService correoDocumentoService;
    private EstadoDocumentoService estadoDocumentoService;
    private DocumentoErrorService documentoErrorService;
    private DocumentoWorkflowService service;

    @BeforeEach
    void setUp() {
        documentoXmlService = mock(DocumentoXmlService.class);
        documentoXmlValidationService = mock(DocumentoXmlValidationService.class);
        archivoDocumentoService = mock(ArchivoDocumentoService.class);
        firmaElectronicaService = mock(FirmaElectronicaService.class);
        sriRecepcionPort = mock(SriRecepcionPort.class);
        sriAutorizacionPort = mock(SriAutorizacionPort.class);
        documentoRideService = mock(DocumentoRideService.class);
        correoDocumentoService = mock(CorreoDocumentoService.class);
        estadoDocumentoService = mock(EstadoDocumentoService.class);
        documentoErrorService = mock(DocumentoErrorService.class);

        service = new DocumentoWorkflowService(
                documentoXmlService,
                documentoXmlValidationService,
                archivoDocumentoService,
                firmaElectronicaService,
                sriRecepcionPort,
                sriAutorizacionPort,
                documentoRideService,
                correoDocumentoService,
                estadoDocumentoService,
                documentoErrorService,
                new ObjectMapper()
        );
    }

    @Test
    void procesaDocumentoAutorizadoHastaFinalizar() throws Exception {
        DocumentoElectronico documento = documentoBase();
        when(documentoXmlService.generar(documento)).thenReturn("<factura><claveAcceso>123</claveAcceso></factura>");
        when(documentoXmlValidationService.validate(documento.getTipoDocumento(), "<factura><claveAcceso>123</claveAcceso></factura>"))
                .thenReturn(new DocumentoXmlValidationService.XmlValidationResult(true, java.util.List.of()));
        when(firmaElectronicaService.firmar(eq(documento), any())).thenReturn("<facturaFirmada><claveAcceso>123</claveAcceso></facturaFirmada>");

        RespuestaSolicitud recepcion = new RespuestaSolicitud();
        recepcion.setEstado("RECIBIDA");
        when(sriRecepcionPort.enviar(eq(documento), any())).thenReturn(recepcion);

        RespuestaComprobante autorizacion = new RespuestaComprobante();
        autorizacion.setNumeroComprobantes("1");
        Autorizacion aut = new Autorizacion();
        aut.setEstado("AUTORIZADO");
        aut.setNumeroAutorizacion("AUT-001");
        aut.setComprobante("<![CDATA[<factura>ok</factura>]]>");
        Autorizaciones autorizaciones = new Autorizaciones();
        autorizaciones.getAutorizacion().add(aut);
        autorizacion.setAutorizaciones(autorizaciones);
        when(sriAutorizacionPort.consultar(documento, "123")).thenReturn(autorizacion);

        when(documentoRideService.generar(documento, "<factura>ok</factura>")).thenReturn(new byte[]{1, 2, 3});

        service.procesar(documento);

        verify(archivoDocumentoService).guardarTexto(documento, DocumentoArchivoTipo.XML_GENERADO, "<factura><claveAcceso>123</claveAcceso></factura>");
        verify(archivoDocumentoService).guardarTexto(documento, DocumentoArchivoTipo.XML_FIRMADO, "<facturaFirmada><claveAcceso>123</claveAcceso></facturaFirmada>");
        verify(archivoDocumentoService).guardarBytes(eq(documento), eq(DocumentoArchivoTipo.XML_AUTORIZADO), any());
        verify(archivoDocumentoService).guardarBytes(documento, DocumentoArchivoTipo.RIDE_PDF, new byte[]{1, 2, 3});
        verify(correoDocumentoService).enviarNotificacionBasica(eq(documento.getEmpresa()), eq("cliente@correo.com"), any(), any());
        verify(estadoDocumentoService).cambiar(documento, DocumentoEstado.FINALIZADO, "Pipeline del documento finalizado");
    }

    @Test
    void fallaSiXmlGeneradoEsInvalido() throws Exception {
        DocumentoElectronico documento = documentoBase();
        when(documentoXmlService.generar(documento)).thenReturn("<factura/>");
        when(documentoXmlValidationService.validate(documento.getTipoDocumento(), "<factura/>"))
                .thenReturn(new DocumentoXmlValidationService.XmlValidationResult(false, java.util.List.of("Campo obligatorio ausente: infoTributaria.claveAcceso")));

        org.junit.jupiter.api.Assertions.assertThrows(IllegalStateException.class, () -> service.procesar(documento));

        verify(documentoErrorService).registrar(eq(documento), eq(com.erp.sri_files.domain.documento.DocumentoEtapa.XML), eq("DOC_XML_ERROR"), eq("Error generando XML"), any(), eq(false));
    }

    private DocumentoElectronico documentoBase() {
        Empresa empresa = new Empresa();
        empresa.setRuc("1790012345001");

        DocumentoElectronico documento = new DocumentoElectronico();
        documento.setUuid(UUID.randomUUID());
        documento.setEmpresa(empresa);
        documento.setTipoDocumento(TipoDocumento.FACTURA);
        documento.setEstadoActual(DocumentoEstado.VALIDADO);
        documento.setCodigoDocumento("01");
        documento.setAmbiente((short) 1);
        documento.setFechaEmision(LocalDate.of(2026, 8, 14));
        documento.setEstablecimiento("001");
        documento.setPuntoEmision("002");
        documento.setSecuencial("000000123");
        documento.setNumeroDocumento("001-002-000000123");
        documento.setClaveAcceso("123");
        documento.setEmailReceptor("cliente@correo.com");
        documento.setRazonSocialReceptor("Cliente Demo");
        documento.setIdentificacionReceptor("0102030405");
        documento.setSubtotal(new BigDecimal("10.00"));
        documento.setImpuestos(new BigDecimal("1.20"));
        documento.setTotal(new BigDecimal("11.20"));
        documento.setMoneda("USD");
        documento.setJsonOriginal("{\"emisor\":{\"ruc\":\"1790012345001\"}}");
        return documento;
    }
}
