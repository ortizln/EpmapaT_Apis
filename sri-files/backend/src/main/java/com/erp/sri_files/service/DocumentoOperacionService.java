package com.erp.sri_files.service;

import com.erp.sri_files.domain.documento.DocumentoArchivoTipo;
import com.erp.sri_files.domain.documento.DocumentoElectronico;
import com.erp.sri_files.domain.documento.DocumentoEstado;
import com.erp.sri_files.dto.response.DocumentoAutorizacionConsultaResponse;
import com.erp.sri_files.dto.response.DocumentoAutorizacionManualResponse;
import com.erp.sri_files.dto.response.DocumentoCorreoReenvioResponse;
import com.erp.sri_files.dto.response.DocumentoOperacionManualResponse;
import com.erp.sri_files.exceptions.DocumentoRecepcionException;
import com.erp.sri_files.repositories.documento.DocumentoElectronicoRepository;
import com.erp.sri_files.sri.port.SriAutorizacionPort;
import com.erp.sri_files.utils.SriAutorizacionAdapter;
import com.fasterxml.jackson.databind.ObjectMapper;
import ec.gob.sri.ws.autorizacion.RespuestaComprobante;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Set;
import java.util.UUID;

@Service
public class DocumentoOperacionService {

    private static final Set<DocumentoEstado> ESTADOS_CONSULTA_AUTORIZACION = Set.of(
            DocumentoEstado.RECIBIDO_SRI,
            DocumentoEstado.PENDIENTE_AUTORIZACION,
            DocumentoEstado.ERROR_AUTORIZACION,
            DocumentoEstado.NO_AUTORIZADO,
            DocumentoEstado.AUTORIZADO,
            DocumentoEstado.RIDE_GENERADO,
            DocumentoEstado.CORREO_PENDIENTE,
            DocumentoEstado.CORREO_ENVIADO,
            DocumentoEstado.FINALIZADO
    );

    private static final Set<DocumentoEstado> ESTADOS_REENVIO_CORREO = Set.of(
            DocumentoEstado.AUTORIZADO,
            DocumentoEstado.RIDE_GENERADO,
            DocumentoEstado.CORREO_PENDIENTE,
            DocumentoEstado.CORREO_ENVIADO,
            DocumentoEstado.ERROR_CORREO,
            DocumentoEstado.FINALIZADO
    );

    private static final Set<DocumentoEstado> ESTADOS_REGENERAR_RIDE = Set.of(
            DocumentoEstado.AUTORIZADO,
            DocumentoEstado.RIDE_GENERADO,
            DocumentoEstado.CORREO_PENDIENTE,
            DocumentoEstado.CORREO_ENVIADO,
            DocumentoEstado.FINALIZADO,
            DocumentoEstado.ERROR_RIDE
    );

    private static final Set<DocumentoEstado> ESTADOS_REPROCESAR = Set.of(
            DocumentoEstado.ERROR_XML,
            DocumentoEstado.ERROR_FIRMA,
            DocumentoEstado.ERROR_ENVIO_SRI,
            DocumentoEstado.ERROR_AUTORIZACION,
            DocumentoEstado.ERROR_RIDE,
            DocumentoEstado.ERROR_CORREO,
            DocumentoEstado.DEVUELTO_SRI,
            DocumentoEstado.NO_AUTORIZADO,
            DocumentoEstado.REQUIERE_INTERVENCION
    );

    private static final Set<DocumentoEstado> ESTADOS_CARGA_XML_MANUAL = Set.of(
            DocumentoEstado.RECIBIDO,
            DocumentoEstado.VALIDANDO,
            DocumentoEstado.VALIDADO,
            DocumentoEstado.XML_GENERADO,
            DocumentoEstado.ERROR_XML,
            DocumentoEstado.ERROR_FIRMA,
            DocumentoEstado.ERROR_ENVIO_SRI,
            DocumentoEstado.ERROR_AUTORIZACION,
            DocumentoEstado.DEVUELTO_SRI,
            DocumentoEstado.NO_AUTORIZADO,
            DocumentoEstado.REQUIERE_INTERVENCION
    );

    private final DocumentoElectronicoRepository documentoRepository;
    private final SriAutorizacionPort sriAutorizacionPort;
    private final ArchivoDocumentoService archivoDocumentoService;
    private final EstadoDocumentoService estadoDocumentoService;
    private final DocumentoWorkflowService documentoWorkflowService;
    private final ObjectMapper objectMapper;

    public DocumentoOperacionService(
            DocumentoElectronicoRepository documentoRepository,
            SriAutorizacionPort sriAutorizacionPort,
            ArchivoDocumentoService archivoDocumentoService,
            EstadoDocumentoService estadoDocumentoService,
            DocumentoWorkflowService documentoWorkflowService,
            ObjectMapper objectMapper
    ) {
        this.documentoRepository = documentoRepository;
        this.sriAutorizacionPort = sriAutorizacionPort;
        this.archivoDocumentoService = archivoDocumentoService;
        this.estadoDocumentoService = estadoDocumentoService;
        this.documentoWorkflowService = documentoWorkflowService;
        this.objectMapper = objectMapper;
    }

    public String describirOperacionDisponible() {
        return "Operaciones administrativas del documento habilitadas";
    }

    public DocumentoAutorizacionConsultaResponse consultarAutorizacionPorClave(String claveAcceso) {
        return consultarAutorizacionPorClave(claveAcceso, false);
    }

    public DocumentoAutorizacionConsultaResponse consultarAutorizacionPorClave(String claveAcceso, boolean incluirXml) {
        if (claveAcceso == null || claveAcceso.isBlank()) {
            throw new DocumentoRecepcionException("La clave de acceso es obligatoria para consultar al SRI");
        }

        try {
            RespuestaComprobante respuesta = sriAutorizacionPort.consultar(claveAcceso.trim());
            var autorizacion = SriAutorizacionAdapter.fromRespuesta(respuesta).orElse(null);

            if (autorizacion == null) {
                return new DocumentoAutorizacionConsultaResponse(
                        claveAcceso.trim(),
                        "SIN_AUTORIZACION_EN_SRI",
                        false,
                        null,
                        null,
                        "Aun no hay autorizaciones disponibles para la clave.",
                        false,
                        null
                );
            }

            String estado = autorizacion.autorizado() ? "AUTORIZADO" : "NO_AUTORIZADO";
            String xmlAutorizado = null;
            if (incluirXml && autorizacion.xmlAutorizado() != null) {
                xmlAutorizado = new String(autorizacion.xmlAutorizado(), StandardCharsets.UTF_8);
            }

            return new DocumentoAutorizacionConsultaResponse(
                    claveAcceso.trim(),
                    estado,
                    autorizacion.autorizado(),
                    autorizacion.numeroAutorizacion(),
                    autorizacion.fechaAutorizacion() == null ? null : autorizacion.fechaAutorizacion().toString(),
                    autorizacion.mensajesConcatenados(),
                    true,
                    xmlAutorizado
            );
        } catch (DocumentoRecepcionException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new DocumentoRecepcionException("No fue posible consultar la autorizacion en el SRI: " + ex.getMessage());
        }
    }

    @Transactional
    public DocumentoAutorizacionManualResponse consultarAutorizacion(UUID uuid) {
        DocumentoElectronico documento = documentoRepository.findByUuid(uuid)
                .orElseThrow(() -> new DocumentoRecepcionException("No existe documento con uuid " + uuid));

        if (!ESTADOS_CONSULTA_AUTORIZACION.contains(documento.getEstadoActual())) {
            throw new DocumentoRecepcionException(
                    "El estado " + documento.getEstadoActual() + " no es compatible con la consulta manual de autorizacion"
            );
        }

        if (documento.getClaveAcceso() == null || documento.getClaveAcceso().isBlank()) {
            throw new DocumentoRecepcionException("El documento no tiene clave de acceso registrada para consultar al SRI");
        }

        try {
            RespuestaComprobante respuesta = sriAutorizacionPort.consultar(documento, documento.getClaveAcceso());
            archivoDocumentoService.guardarTexto(documento, DocumentoArchivoTipo.RESPUESTA_SRI, objectMapper.writeValueAsString(respuesta));

            var autorizacion = SriAutorizacionAdapter.fromRespuesta(respuesta).orElse(null);
            if (autorizacion == null) {
                documento.setMensajeSri("Sin respuesta util de autorizacion");
                documentoRepository.save(documento);
                return new DocumentoAutorizacionManualResponse(
                        documento.getUuid().toString(),
                        documento.getClaveAcceso(),
                        documento.getEstadoActual().name(),
                        false,
                        documento.getNumeroAutorizacion(),
                        documento.getFechaAutorizacion() == null ? null : documento.getFechaAutorizacion().toString(),
                        "Sin respuesta util de autorizacion",
                        false
                );
            }

            boolean actualizado = false;
            documento.setMensajeSri(autorizacion.mensajesConcatenados());

            if (autorizacion.autorizado()) {
                documento.setNumeroAutorizacion(autorizacion.numeroAutorizacion());
                documento.setFechaAutorizacion(autorizacion.fechaAutorizacion());
                if (autorizacion.xmlAutorizado() != null) {
                    archivoDocumentoService.guardarBytes(documento, DocumentoArchivoTipo.XML_AUTORIZADO, autorizacion.xmlAutorizado());
                }

                if (documento.getEstadoActual() == DocumentoEstado.RECIBIDO_SRI) {
                    estadoDocumentoService.cambiar(documento, DocumentoEstado.PENDIENTE_AUTORIZACION, "Consulta manual de autorizacion iniciada");
                    actualizado = true;
                }

                if (documento.getEstadoActual() == DocumentoEstado.PENDIENTE_AUTORIZACION
                        || documento.getEstadoActual() == DocumentoEstado.ERROR_AUTORIZACION
                        || documento.getEstadoActual() == DocumentoEstado.NO_AUTORIZADO) {
                    estadoDocumentoService.cambiar(documento, DocumentoEstado.AUTORIZADO, "Documento autorizado por SRI en consulta manual");
                    actualizado = true;
                } else {
                    documentoRepository.save(documento);
                }
            } else {
                if (documento.getEstadoActual() == DocumentoEstado.RECIBIDO_SRI) {
                    estadoDocumentoService.cambiar(documento, DocumentoEstado.PENDIENTE_AUTORIZACION, "Consulta manual de autorizacion iniciada");
                    actualizado = true;
                }

                if (documento.getEstadoActual() == DocumentoEstado.PENDIENTE_AUTORIZACION
                        || documento.getEstadoActual() == DocumentoEstado.ERROR_AUTORIZACION) {
                    estadoDocumentoService.cambiar(documento, DocumentoEstado.NO_AUTORIZADO, "Documento no autorizado por SRI en consulta manual");
                    actualizado = true;
                } else {
                    documentoRepository.save(documento);
                }
            }

            return new DocumentoAutorizacionManualResponse(
                    documento.getUuid().toString(),
                    documento.getClaveAcceso(),
                    documento.getEstadoActual().name(),
                    autorizacion.autorizado(),
                    documento.getNumeroAutorizacion(),
                    documento.getFechaAutorizacion() == null ? null : documento.getFechaAutorizacion().toString(),
                    documento.getMensajeSri(),
                    actualizado
            );
        } catch (DocumentoRecepcionException ex) {
            throw ex;
        } catch (Exception ex) {
            documento.setMensajeSri("Error consultando autorizacion SRI: " + ex.getMessage());
            if (documento.getEstadoActual() == DocumentoEstado.PENDIENTE_AUTORIZACION) {
                estadoDocumentoService.cambiar(documento, DocumentoEstado.ERROR_AUTORIZACION, "Consulta manual de autorizacion con error");
            } else {
                documentoRepository.save(documento);
            }
            throw new DocumentoRecepcionException("No fue posible consultar la autorizacion en el SRI: " + ex.getMessage());
        }
    }

    @Transactional
    public DocumentoCorreoReenvioResponse reenviarCorreo(UUID uuid) {
        DocumentoElectronico documento = documentoRepository.findByUuid(uuid)
                .orElseThrow(() -> new DocumentoRecepcionException("No existe documento con uuid " + uuid));

        if (!ESTADOS_REENVIO_CORREO.contains(documento.getEstadoActual())) {
            throw new DocumentoRecepcionException(
                    "El estado " + documento.getEstadoActual() + " no es compatible con el reenvio de correo"
            );
        }

        if (documento.getEmailReceptor() == null || documento.getEmailReceptor().isBlank()) {
            throw new DocumentoRecepcionException("El documento no tiene correo receptor configurado para reenviar");
        }

        try {
            if (documento.getEstadoActual() == DocumentoEstado.AUTORIZADO || documento.getEstadoActual() == DocumentoEstado.RIDE_GENERADO) {
                estadoDocumentoService.cambiar(documento, DocumentoEstado.CORREO_PENDIENTE, "Reenvio manual de correo solicitado");
            }

            documentoWorkflowService.reenviarCorreo(documento);
            estadoDocumentoService.cambiar(documento, DocumentoEstado.CORREO_ENVIADO, "Correo reenviado manualmente");

            return new DocumentoCorreoReenvioResponse(
                    documento.getUuid().toString(),
                    documento.getEstadoActual().name(),
                    documento.getEmailReceptor(),
                    "Correo reenviado correctamente"
            );
        } catch (DocumentoRecepcionException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new DocumentoRecepcionException("No fue posible reenviar el correo del documento: " + ex.getMessage());
        }
    }

    @Transactional
    public DocumentoOperacionManualResponse regenerarRide(UUID uuid, String motivo) {
        DocumentoElectronico documento = documentoRepository.findByUuid(uuid)
                .orElseThrow(() -> new DocumentoRecepcionException("No existe documento con uuid " + uuid));

        if (!ESTADOS_REGENERAR_RIDE.contains(documento.getEstadoActual())) {
            throw new DocumentoRecepcionException(
                    "El estado " + documento.getEstadoActual() + " no es compatible con regenerar el RIDE"
            );
        }

        DocumentoEstado estadoAnterior = documento.getEstadoActual();

        try {
            byte[] xmlAutorizado = archivoDocumentoService.leer(uuid, DocumentoArchivoTipo.XML_AUTORIZADO);
            byte[] ride = documentoWorkflowService.regenerarRide(documento, new String(xmlAutorizado, StandardCharsets.UTF_8));
            archivoDocumentoService.guardarBytes(documento, DocumentoArchivoTipo.RIDE_PDF, ride);

            if (estadoAnterior == DocumentoEstado.ERROR_RIDE) {
                estadoDocumentoService.forzarCambio(
                        documento,
                        DocumentoEstado.RIDE_GENERADO,
                        "RIDE regenerado manualmente" + complementarMotivo(motivo),
                        "MANUAL"
                );
            }

            return new DocumentoOperacionManualResponse(
                    documento.getUuid().toString(),
                    estadoAnterior.name(),
                    documento.getEstadoActual().name(),
                    "REGENERAR_RIDE",
                    "RIDE regenerado correctamente"
            );
        } catch (Exception ex) {
            throw new DocumentoRecepcionException("No fue posible regenerar el RIDE: " + ex.getMessage());
        }
    }

    @Transactional
    public DocumentoOperacionManualResponse reprocesar(UUID uuid, String motivo) {
        DocumentoElectronico documento = documentoRepository.findByUuid(uuid)
                .orElseThrow(() -> new DocumentoRecepcionException("No existe documento con uuid " + uuid));

        if (!ESTADOS_REPROCESAR.contains(documento.getEstadoActual())) {
            throw new DocumentoRecepcionException(
                    "El estado " + documento.getEstadoActual() + " no es compatible con reprocesamiento"
            );
        }

        DocumentoEstado estadoAnterior = documento.getEstadoActual();

        if (estadoAnterior == DocumentoEstado.ERROR_RIDE) {
            return regenerarRide(uuid, motivo);
        }

        if (estadoAnterior == DocumentoEstado.ERROR_CORREO) {
            DocumentoCorreoReenvioResponse correo = reenviarCorreo(uuid);
            return new DocumentoOperacionManualResponse(
                    correo.id(),
                    estadoAnterior.name(),
                    correo.estado(),
                    "REENVIAR_CORREO",
                    correo.mensaje()
            );
        }

        estadoDocumentoService.forzarCambio(
                documento,
                DocumentoEstado.VALIDADO,
                "Documento marcado para reproceso manual" + complementarMotivo(motivo),
                "MANUAL"
        );
        documentoWorkflowService.procesar(documento);

        return new DocumentoOperacionManualResponse(
                documento.getUuid().toString(),
                estadoAnterior.name(),
                documento.getEstadoActual().name(),
                "REPROCESAR",
                "Documento reprocesado correctamente"
        );
    }

    @Transactional
    public DocumentoOperacionManualResponse cargarXmlSinFirmar(UUID uuid, MultipartFile xmlFile, String motivo) {
        DocumentoElectronico documento = documentoRepository.findByUuid(uuid)
                .orElseThrow(() -> new DocumentoRecepcionException("No existe documento con uuid " + uuid));

        if (!ESTADOS_CARGA_XML_MANUAL.contains(documento.getEstadoActual())) {
            throw new DocumentoRecepcionException(
                    "El estado " + documento.getEstadoActual() + " no es compatible con carga manual de XML"
            );
        }

        if (xmlFile == null || xmlFile.isEmpty()) {
            throw new DocumentoRecepcionException("Debes adjuntar un archivo XML sin firmar.");
        }

        DocumentoEstado estadoAnterior = documento.getEstadoActual();

        try {
            String xmlPlano = new String(xmlFile.getBytes(), StandardCharsets.UTF_8);
            if (!xmlPlano.isEmpty() && xmlPlano.charAt(0) == '\uFEFF') {
                xmlPlano = xmlPlano.substring(1);
            }

            estadoDocumentoService.forzarCambio(
                    documento,
                    DocumentoEstado.VALIDADO,
                    "Documento preparado para flujo manual con XML cargado" + complementarMotivo(motivo),
                    "MANUAL_XML"
            );
            documentoWorkflowService.procesarDesdeXmlGenerado(documento, xmlPlano.trim());

            return new DocumentoOperacionManualResponse(
                    documento.getUuid().toString(),
                    estadoAnterior.name(),
                    documento.getEstadoActual().name(),
                    "CARGAR_XML_SIN_FIRMAR",
                    "XML cargado, firmado y enviado correctamente."
            );
        } catch (DocumentoRecepcionException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new DocumentoRecepcionException("No fue posible procesar el XML sin firmar: " + ex.getMessage());
        }
    }

    @Transactional(readOnly = true)
    public byte[] descargarXml(UUID uuid) {
        return archivoDocumentoService.leer(uuid, DocumentoArchivoTipo.XML_GENERADO);
    }

    @Transactional(readOnly = true)
    public byte[] descargarXmlFirmado(UUID uuid) {
        return archivoDocumentoService.leer(uuid, DocumentoArchivoTipo.XML_FIRMADO);
    }

    @Transactional(readOnly = true)
    public byte[] descargarXmlAutorizado(UUID uuid) {
        return archivoDocumentoService.leer(uuid, DocumentoArchivoTipo.XML_AUTORIZADO);
    }

    @Transactional(readOnly = true)
    public byte[] descargarRide(UUID uuid) {
        return archivoDocumentoService.leer(uuid, DocumentoArchivoTipo.RIDE_PDF);
    }

    @Transactional(readOnly = true)
    public String nombreArchivo(UUID uuid, DocumentoArchivoTipo tipoArchivo) {
        return archivoDocumentoService.nombreDescarga(uuid, tipoArchivo);
    }

    @Transactional(readOnly = true)
    public String mimeType(UUID uuid, DocumentoArchivoTipo tipoArchivo) {
        return archivoDocumentoService.mimeType(uuid, tipoArchivo);
    }

    private String complementarMotivo(String motivo) {
        return motivo == null || motivo.isBlank() ? "" : ". Motivo: " + motivo.trim();
    }
}
