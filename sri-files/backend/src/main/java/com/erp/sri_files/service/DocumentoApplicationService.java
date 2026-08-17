package com.erp.sri_files.service;

import com.erp.sri_files.dto.response.DocumentoAutorizacionConsultaResponse;
import com.erp.sri_files.dto.request.DocumentoRecepcionRequest;
import com.erp.sri_files.dto.response.DocumentoDetalleResponse;
import com.erp.sri_files.dto.response.DocumentoAutorizacionManualResponse;
import com.erp.sri_files.dto.response.DocumentoAuditoriaResumenResponse;
import com.erp.sri_files.dto.response.DocumentoCorreoSeguimientoResponse;
import com.erp.sri_files.dto.response.DocumentoCorreoReenvioResponse;
import com.erp.sri_files.dto.response.DocumentoEstadoResponse;
import com.erp.sri_files.dto.response.DocumentoErrorItemResponse;
import com.erp.sri_files.dto.response.DocumentoHistorialItemResponse;
import com.erp.sri_files.dto.response.DocumentoIntentoSriResponse;
import com.erp.sri_files.dto.response.DocumentoListadoResponse;
import com.erp.sri_files.dto.response.DocumentoRecepcionResponse;
import com.erp.sri_files.dto.response.DocumentoResumenOperativoResponse;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
public class DocumentoApplicationService {

    private final DocumentoRecepcionService documentoRecepcionService;
    private final DocumentoConsultaService documentoConsultaService;
    private final DocumentoOperacionService documentoOperacionService;

    public DocumentoApplicationService(
            DocumentoRecepcionService documentoRecepcionService,
            DocumentoConsultaService documentoConsultaService,
            DocumentoOperacionService documentoOperacionService
    ) {
        this.documentoRecepcionService = documentoRecepcionService;
        this.documentoConsultaService = documentoConsultaService;
        this.documentoOperacionService = documentoOperacionService;
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
}
