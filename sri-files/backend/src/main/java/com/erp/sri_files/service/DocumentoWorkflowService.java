package com.erp.sri_files.service;

import com.erp.sri_files.domain.documento.DocumentoArchivoTipo;
import com.erp.sri_files.domain.documento.DocumentoElectronico;
import com.erp.sri_files.domain.documento.DocumentoEstado;
import com.erp.sri_files.domain.documento.DocumentoEtapa;
import com.erp.sri_files.mail.CorreoDocumentoService;
import com.erp.sri_files.signature.FirmaElectronicaService;
import com.erp.sri_files.sri.port.SriAutorizacionPort;
import com.erp.sri_files.sri.port.SriRecepcionPort;
import com.erp.sri_files.utils.SriAutorizacionAdapter;
import com.fasterxml.jackson.databind.ObjectMapper;
import ec.gob.sri.ws.autorizacion.RespuestaComprobante;
import ec.gob.sri.ws.recepcion.RespuestaSolicitud;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class DocumentoWorkflowService {

    private final DocumentoXmlService documentoXmlService;
    private final DocumentoXmlValidationService documentoXmlValidationService;
    private final ArchivoDocumentoService archivoDocumentoService;
    private final FirmaElectronicaService firmaElectronicaService;
    private final SriRecepcionPort sriRecepcionPort;
    private final SriAutorizacionPort sriAutorizacionPort;
    private final DocumentoRideService documentoRideService;
    private final CorreoDocumentoService correoDocumentoService;
    private final EstadoDocumentoService estadoDocumentoService;
    private final DocumentoErrorService documentoErrorService;
    private final ObjectMapper objectMapper;

    public DocumentoWorkflowService(
            DocumentoXmlService documentoXmlService,
            DocumentoXmlValidationService documentoXmlValidationService,
            ArchivoDocumentoService archivoDocumentoService,
            FirmaElectronicaService firmaElectronicaService,
            SriRecepcionPort sriRecepcionPort,
            SriAutorizacionPort sriAutorizacionPort,
            DocumentoRideService documentoRideService,
            CorreoDocumentoService correoDocumentoService,
            EstadoDocumentoService estadoDocumentoService,
            DocumentoErrorService documentoErrorService,
            ObjectMapper objectMapper
    ) {
        this.documentoXmlService = documentoXmlService;
        this.documentoXmlValidationService = documentoXmlValidationService;
        this.archivoDocumentoService = archivoDocumentoService;
        this.firmaElectronicaService = firmaElectronicaService;
        this.sriRecepcionPort = sriRecepcionPort;
        this.sriAutorizacionPort = sriAutorizacionPort;
        this.documentoRideService = documentoRideService;
        this.correoDocumentoService = correoDocumentoService;
        this.estadoDocumentoService = estadoDocumentoService;
        this.documentoErrorService = documentoErrorService;
        this.objectMapper = objectMapper;
    }

    public void procesar(DocumentoElectronico documento) {
        documento.setFechaInicioProcesamiento(LocalDateTime.now());

        String xmlGenerado = ejecutarEtapaXml(documento);
        String xmlFirmado = ejecutarEtapaFirma(documento, xmlGenerado);
        RespuestaSolicitud recepcion = ejecutarEtapaRecepcion(documento, xmlFirmado);

        if (!"RECIBIDA".equalsIgnoreCase(recepcion.getEstado())) {
            documento.setMensajeSri(recepcion.getEstado());
            estadoDocumentoService.cambiar(documento, DocumentoEstado.DEVUELTO_SRI, "SRI devolvio el comprobante en recepcion");
            documento.setFechaFinalizacion(LocalDateTime.now());
            return;
        }

        estadoDocumentoService.cambiar(documento, DocumentoEstado.RECIBIDO_SRI, "Recepcion SRI confirmada");
        estadoDocumentoService.cambiar(documento, DocumentoEstado.PENDIENTE_AUTORIZACION, "Esperando autorizacion SRI");

        RespuestaComprobante autorizacion = ejecutarEtapaAutorizacion(documento);
        var autorizacionInfo = SriAutorizacionAdapter.fromRespuesta(autorizacion).orElse(null);
        if (autorizacionInfo == null) {
            documento.setMensajeSri("Sin respuesta util de autorizacion");
            documento.setFechaFinalizacion(LocalDateTime.now());
            return;
        }

        documento.setMensajeSri(autorizacionInfo.mensajesConcatenados());
        if (!autorizacionInfo.autorizado()) {
            estadoDocumentoService.cambiar(documento, DocumentoEstado.NO_AUTORIZADO, "Documento no autorizado por SRI");
            documento.setFechaFinalizacion(LocalDateTime.now());
            return;
        }

        documento.setNumeroAutorizacion(autorizacionInfo.numeroAutorizacion());
        documento.setFechaAutorizacion(autorizacionInfo.fechaAutorizacion());
        archivoDocumentoService.guardarBytes(documento, DocumentoArchivoTipo.XML_AUTORIZADO, autorizacionInfo.xmlAutorizado());
        estadoDocumentoService.cambiar(documento, DocumentoEstado.AUTORIZADO, "Documento autorizado por SRI");

        byte[] ride = ejecutarEtapaRide(documento, new String(autorizacionInfo.xmlAutorizado(), java.nio.charset.StandardCharsets.UTF_8));
        archivoDocumentoService.guardarBytes(documento, DocumentoArchivoTipo.RIDE_PDF, ride);
        estadoDocumentoService.cambiar(documento, DocumentoEstado.RIDE_GENERADO, "RIDE generado");

        if (documento.getEmailReceptor() != null && !documento.getEmailReceptor().isBlank()) {
            estadoDocumentoService.cambiar(documento, DocumentoEstado.CORREO_PENDIENTE, "Pendiente notificacion al receptor");
            ejecutarEtapaCorreo(documento);
            estadoDocumentoService.cambiar(documento, DocumentoEstado.CORREO_ENVIADO, "Correo de documento enviado");
        }

        documento.setFechaFinalizacion(LocalDateTime.now());
        estadoDocumentoService.cambiar(documento, DocumentoEstado.FINALIZADO, "Pipeline del documento finalizado");
    }

    private String ejecutarEtapaXml(DocumentoElectronico documento) {
        try {
            String xmlGenerado = documentoXmlService.generar(documento);
            var validation = documentoXmlValidationService.validate(documento.getTipoDocumento(), xmlGenerado);
            if (!validation.valid()) {
                throw new IllegalStateException("XML invalido: " + String.join(" | ", validation.errors()));
            }
            archivoDocumentoService.guardarTexto(documento, DocumentoArchivoTipo.XML_GENERADO, xmlGenerado);
            estadoDocumentoService.cambiar(documento, DocumentoEstado.XML_GENERADO, "XML del documento generado");
            return xmlGenerado;
        } catch (Exception ex) {
            documentoErrorService.registrar(documento, DocumentoEtapa.XML, "DOC_XML_ERROR", "Error generando XML", asException(ex), false);
            estadoDocumentoService.cambiar(documento, DocumentoEstado.ERROR_XML, "Error al generar XML: " + ex.getMessage());
            throw ex;
        }
    }

    private String ejecutarEtapaFirma(DocumentoElectronico documento, String xmlGenerado) {
        try {
            String xmlFirmado = firmaElectronicaService.firmar(documento, xmlGenerado);
            archivoDocumentoService.guardarTexto(documento, DocumentoArchivoTipo.XML_FIRMADO, xmlFirmado);
            estadoDocumentoService.cambiar(documento, DocumentoEstado.FIRMADO, "XML firmado electronicamente");
            return xmlFirmado;
        } catch (Exception ex) {
            documentoErrorService.registrar(documento, DocumentoEtapa.FIRMA, "DOC_SIGNATURE_ERROR", "Error firmando XML", asException(ex), false);
            estadoDocumentoService.cambiar(documento, DocumentoEstado.ERROR_FIRMA, "Error al firmar XML: " + ex.getMessage());
            throw ex;
        }
    }

    private RespuestaSolicitud ejecutarEtapaRecepcion(DocumentoElectronico documento, String xmlFirmado) {
        try {
            estadoDocumentoService.cambiar(documento, DocumentoEstado.ENVIANDO_SRI, "Enviando comprobante al SRI");
            RespuestaSolicitud respuesta = sriRecepcionPort.enviar(documento, xmlFirmado);
            archivoDocumentoService.guardarTexto(documento, DocumentoArchivoTipo.RESPUESTA_SRI, objectMapper.writeValueAsString(respuesta));
            return respuesta;
        } catch (Exception ex) {
            documentoErrorService.registrar(documento, DocumentoEtapa.ENVIO_SRI, "DOC_SRI_SEND_ERROR", "Error en recepcion SRI", asException(ex), true);
            estadoDocumentoService.cambiar(documento, DocumentoEstado.ERROR_ENVIO_SRI, "Error enviando comprobante al SRI: " + ex.getMessage());
            throw new IllegalStateException(ex);
        }
    }

    private RespuestaComprobante ejecutarEtapaAutorizacion(DocumentoElectronico documento) {
        try {
            RespuestaComprobante respuesta = sriAutorizacionPort.consultar(documento, documento.getClaveAcceso());
            archivoDocumentoService.guardarTexto(documento, DocumentoArchivoTipo.RESPUESTA_SRI, objectMapper.writeValueAsString(respuesta));
            return respuesta;
        } catch (Exception ex) {
            documentoErrorService.registrar(documento, DocumentoEtapa.AUTORIZACION, "DOC_SRI_AUTH_ERROR", "Error consultando autorizacion", asException(ex), true);
            estadoDocumentoService.cambiar(documento, DocumentoEstado.ERROR_AUTORIZACION, "Error consultando autorizacion SRI: " + ex.getMessage());
            throw new IllegalStateException(ex);
        }
    }

    private byte[] ejecutarEtapaRide(DocumentoElectronico documento, String xmlAutorizado) {
        try {
            return documentoRideService.generar(documento, xmlAutorizado);
        } catch (Exception ex) {
            documentoErrorService.registrar(documento, DocumentoEtapa.RIDE, "DOC_RIDE_ERROR", "Error generando RIDE", asException(ex), false);
            estadoDocumentoService.cambiar(documento, DocumentoEstado.ERROR_RIDE, "Error generando RIDE: " + ex.getMessage());
            throw new IllegalStateException(ex);
        }
    }

    public void reenviarCorreo(DocumentoElectronico documento) {
        ejecutarEtapaCorreo(documento);
    }

    private void ejecutarEtapaCorreo(DocumentoElectronico documento) {
        try {
            correoDocumentoService.enviarNotificacionBasica(
                    documento.getEmpresa(),
                    documento.getEmailReceptor(),
                    "Documento electronico autorizado",
                    "Su comprobante " + documento.getNumeroDocumento() + " ha sido autorizado por el SRI."
            );
        } catch (Exception ex) {
            documentoErrorService.registrar(documento, DocumentoEtapa.CORREO, "DOC_EMAIL_ERROR", "Error enviando correo", asException(ex), true);
            estadoDocumentoService.cambiar(documento, DocumentoEstado.ERROR_CORREO, "Error notificando al receptor: " + ex.getMessage());
            throw new IllegalStateException(ex);
        }
    }

    private Exception asException(Exception ex) {
        return ex;
    }

    private Exception asException(Throwable ex) {
        return ex instanceof Exception exception ? exception : new Exception(ex);
    }
}
