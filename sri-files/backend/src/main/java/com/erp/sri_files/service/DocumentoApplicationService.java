package com.erp.sri_files.service;

import com.erp.sri_files.dto.response.DocumentoAutorizacionConsultaResponse;
import com.erp.sri_files.dto.request.DocumentoRecepcionRequest;
import com.erp.sri_files.dto.response.DocumentoDetalleResponse;
import com.erp.sri_files.dto.response.DocumentoAutorizacionManualResponse;
import com.erp.sri_files.dto.response.DocumentoAuditoriaResumenResponse;
import com.erp.sri_files.dto.response.DocumentoArchivoItemResponse;
import com.erp.sri_files.dto.response.DocumentoCorreoSeguimientoResponse;
import com.erp.sri_files.dto.response.DocumentoCorreoReenvioResponse;
import com.erp.sri_files.dto.response.DocumentoEstadoResponse;
import com.erp.sri_files.dto.response.DocumentoErrorItemResponse;
import com.erp.sri_files.dto.response.DocumentoHistorialItemResponse;
import com.erp.sri_files.dto.response.DocumentoIntentoSriResponse;
import com.erp.sri_files.dto.response.DocumentoListadoResponse;
import com.erp.sri_files.dto.response.DocumentoOperacionManualResponse;
import com.erp.sri_files.dto.response.DocumentoRecepcionResponse;
import com.erp.sri_files.dto.response.DocumentoResumenOperativoResponse;
import com.erp.sri_files.dto.response.RideContratoDocumentoResponse;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

@Service
public class DocumentoApplicationService {

    private final DocumentoRecepcionService documentoRecepcionService;
    private final DocumentoConsultaService documentoConsultaService;
    private final DocumentoOperacionService documentoOperacionService;
    private final PlantillaRidePreviewService plantillaRidePreviewService;
    private final RideContratoService rideContratoService;

    public DocumentoApplicationService(
            DocumentoRecepcionService documentoRecepcionService,
            DocumentoConsultaService documentoConsultaService,
            DocumentoOperacionService documentoOperacionService,
            PlantillaRidePreviewService plantillaRidePreviewService,
            RideContratoService rideContratoService
    ) {
        this.documentoRecepcionService = documentoRecepcionService;
        this.documentoConsultaService = documentoConsultaService;
        this.documentoOperacionService = documentoOperacionService;
        this.plantillaRidePreviewService = plantillaRidePreviewService;
        this.rideContratoService = rideContratoService;
    }

    public DocumentoRecepcionResponse recibir(DocumentoRecepcionRequest request, String idempotencyKey) {
        return documentoRecepcionService.recibir(request, idempotencyKey);
    }

    public DocumentoDetalleResponse obtener(UUID uuid) {
        return documentoConsultaService.obtener(uuid);
    }

    public DocumentoEstadoResponse obtenerEstado(UUID uuid) {
        return documentoConsultaService.obtenerEstado(uuid);
    }

    public DocumentoListadoResponse listar(String empresaUuid, String tipoDocumento, String estado, String busqueda, int page, int size) {
        return documentoConsultaService.listar(empresaUuid, tipoDocumento, estado, busqueda, page, size);
    }

    public DocumentoListadoResponse buscarRapido(String q, int page, int size) {
        return documentoConsultaService.buscarRapido(q, page, size);
    }

    public byte[] exportarCsv(String empresaUuid, String tipoDocumento, String estado, String busqueda) {
        return documentoConsultaService.exportarCsv(empresaUuid, tipoDocumento, estado, busqueda);
    }

    public DocumentoResumenOperativoResponse obtenerResumenOperativo() {
        return documentoConsultaService.obtenerResumenOperativo();
    }

    public DocumentoAutorizacionConsultaResponse consultarAutorizacionPorClave(String claveAcceso, boolean incluirXml) {
        return documentoOperacionService.consultarAutorizacionPorClave(claveAcceso, incluirXml);
    }

    public DocumentoAutorizacionManualResponse consultarAutorizacion(UUID uuid) {
        return documentoOperacionService.consultarAutorizacion(uuid);
    }

    public List<DocumentoHistorialItemResponse> obtenerHistorial(UUID uuid) {
        return documentoConsultaService.obtenerHistorial(uuid);
    }

    public DocumentoAuditoriaResumenResponse obtenerAuditoriaReciente() {
        return documentoConsultaService.obtenerAuditoriaReciente();
    }

    public List<DocumentoErrorItemResponse> obtenerErrores(UUID uuid) {
        return documentoConsultaService.obtenerErrores(uuid);
    }

    public DocumentoIntentoSriResponse obtenerIntentosSri(UUID uuid) {
        return documentoConsultaService.obtenerIntentosSri(uuid);
    }

    public DocumentoCorreoSeguimientoResponse obtenerSeguimientoCorreo(UUID uuid) {
        return documentoConsultaService.obtenerSeguimientoCorreo(uuid);
    }

    public DocumentoCorreoReenvioResponse reenviarCorreo(UUID uuid) {
        return documentoOperacionService.reenviarCorreo(uuid);
    }

    public java.util.List<DocumentoArchivoItemResponse> listarArchivos(UUID uuid) {
        return documentoConsultaService.listarArchivos(uuid);
    }

    public byte[] descargarXml(UUID uuid) {
        return documentoOperacionService.descargarXml(uuid);
    }

    public byte[] descargarXmlFirmado(UUID uuid) {
        return documentoOperacionService.descargarXmlFirmado(uuid);
    }

    public byte[] descargarXmlAutorizado(UUID uuid) {
        return documentoOperacionService.descargarXmlAutorizado(uuid);
    }

    public byte[] descargarRide(UUID uuid) {
        return documentoOperacionService.descargarRide(uuid);
    }

    public String nombreArchivo(UUID uuid, com.erp.sri_files.domain.documento.DocumentoArchivoTipo tipoArchivo) {
        return documentoOperacionService.nombreArchivo(uuid, tipoArchivo);
    }

    public String mimeType(UUID uuid, com.erp.sri_files.domain.documento.DocumentoArchivoTipo tipoArchivo) {
        return documentoOperacionService.mimeType(uuid, tipoArchivo);
    }

    public DocumentoOperacionManualResponse reprocesar(UUID uuid, String motivo) {
        return documentoOperacionService.reprocesar(uuid, motivo);
    }

    public DocumentoOperacionManualResponse cargarXmlSinFirmar(UUID uuid, MultipartFile xmlFile, String motivo) {
        return documentoOperacionService.cargarXmlSinFirmar(uuid, xmlFile, motivo);
    }

    public DocumentoOperacionManualResponse regenerarRide(UUID uuid, String motivo) {
        return documentoOperacionService.regenerarRide(uuid, motivo);
    }

    public byte[] previewPlantillaRide(UUID plantillaUuid, UUID documentoUuid) {
        return plantillaRidePreviewService.generarPreview(plantillaUuid, documentoUuid);
    }

    public RideContratoDocumentoResponse obtenerContratoRide(UUID documentoUuid) {
        return rideContratoService.obtener(documentoUuid);
    }
}
