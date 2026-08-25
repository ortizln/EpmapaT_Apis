package com.erp.sri_files.controller;

import com.erp.sri_files.dto.request.DocumentoRecepcionRequest;
import com.erp.sri_files.dto.request.DocumentoReprocesarRequest;
import com.erp.sri_files.dto.response.DocumentoAutorizacionConsultaResponse;
import com.erp.sri_files.dto.response.DocumentoArchivoItemResponse;
import com.erp.sri_files.dto.response.DocumentoContratoResponse;
import com.erp.sri_files.dto.response.DocumentoDetalleResponse;
import com.erp.sri_files.dto.response.DocumentoAuditoriaResumenResponse;
import com.erp.sri_files.dto.response.DocumentoAutorizacionManualResponse;
import com.erp.sri_files.dto.response.DocumentoCorreoSeguimientoResponse;
import com.erp.sri_files.dto.response.DocumentoCorreoReenvioResponse;
import com.erp.sri_files.dto.response.DocumentoErrorItemResponse;
import com.erp.sri_files.dto.response.DocumentoRecepcionResponse;
import com.erp.sri_files.dto.response.DocumentoEstadoResponse;
import com.erp.sri_files.dto.response.DocumentoHistorialItemResponse;
import com.erp.sri_files.dto.response.DocumentoIntentoSriResponse;
import com.erp.sri_files.dto.response.DocumentoListadoResponse;
import com.erp.sri_files.dto.response.DocumentoOperacionManualResponse;
import com.erp.sri_files.dto.response.DocumentoResumenOperativoResponse;
import com.erp.sri_files.dto.response.RideContratoDocumentoResponse;
import com.erp.sri_files.service.DocumentoApplicationService;
import com.erp.sri_files.service.DocumentoContratoService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/documentos")
public class DocumentoController {

    private final DocumentoApplicationService documentoApplicationService;
    private final DocumentoContratoService documentoContratoService;

    public DocumentoController(
            DocumentoApplicationService documentoApplicationService,
            DocumentoContratoService documentoContratoService
    ) {
        this.documentoApplicationService = documentoApplicationService;
        this.documentoContratoService = documentoContratoService;
    }

    @PostMapping
    public ResponseEntity<DocumentoRecepcionResponse> recibirDocumento(
            @Valid @RequestBody DocumentoRecepcionRequest request,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey
    ) {
        DocumentoRecepcionResponse response = documentoApplicationService.recibir(request, idempotencyKey);
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(response);
    }

    @GetMapping("/{uuid}")
    public ResponseEntity<DocumentoDetalleResponse> obtenerDocumento(@PathVariable UUID uuid) {
        return ResponseEntity.ok(documentoApplicationService.obtener(uuid));
    }

    @GetMapping("/{uuid}/estado")
    public ResponseEntity<DocumentoEstadoResponse> obtenerEstado(@PathVariable UUID uuid) {
        return ResponseEntity.ok(documentoApplicationService.obtenerEstado(uuid));
    }

    @GetMapping("/{uuid}/historial")
    public ResponseEntity<List<DocumentoHistorialItemResponse>> obtenerHistorial(@PathVariable UUID uuid) {
        return ResponseEntity.ok(documentoApplicationService.obtenerHistorial(uuid));
    }

    @GetMapping("/auditoria")
    public ResponseEntity<DocumentoAuditoriaResumenResponse> obtenerAuditoriaReciente() {
        return ResponseEntity.ok(documentoApplicationService.obtenerAuditoriaReciente());
    }

    @GetMapping("/{uuid}/errores")
    public ResponseEntity<List<DocumentoErrorItemResponse>> obtenerErrores(@PathVariable UUID uuid) {
        return ResponseEntity.ok(documentoApplicationService.obtenerErrores(uuid));
    }

    @GetMapping("/{uuid}/intentos-sri")
    public ResponseEntity<DocumentoIntentoSriResponse> obtenerIntentosSri(@PathVariable UUID uuid) {
        return ResponseEntity.ok(documentoApplicationService.obtenerIntentosSri(uuid));
    }

    @GetMapping("/{uuid}/archivos")
    public ResponseEntity<List<DocumentoArchivoItemResponse>> listarArchivos(@PathVariable UUID uuid) {
        return ResponseEntity.ok(documentoApplicationService.listarArchivos(uuid));
    }

    @GetMapping("/{uuid}/xml")
    public ResponseEntity<byte[]> descargarXml(@PathVariable UUID uuid) {
        return descargar(uuid, com.erp.sri_files.domain.documento.DocumentoArchivoTipo.XML_GENERADO);
    }

    @GetMapping("/{uuid}/xml-firmado")
    public ResponseEntity<byte[]> descargarXmlFirmado(@PathVariable UUID uuid) {
        return descargar(uuid, com.erp.sri_files.domain.documento.DocumentoArchivoTipo.XML_FIRMADO);
    }

    @GetMapping("/{uuid}/xml-autorizado")
    public ResponseEntity<byte[]> descargarXmlAutorizado(@PathVariable UUID uuid) {
        return descargar(uuid, com.erp.sri_files.domain.documento.DocumentoArchivoTipo.XML_AUTORIZADO);
    }

    @GetMapping("/{uuid}/ride")
    public ResponseEntity<byte[]> descargarRide(@PathVariable UUID uuid) {
        return descargar(uuid, com.erp.sri_files.domain.documento.DocumentoArchivoTipo.RIDE_PDF);
    }

    @GetMapping("/{uuid}/ride/contrato")
    public ResponseEntity<RideContratoDocumentoResponse> obtenerContratoRide(@PathVariable UUID uuid) {
        return ResponseEntity.ok(documentoApplicationService.obtenerContratoRide(uuid));
    }

    @GetMapping("/{uuid}/correo")
    public ResponseEntity<DocumentoCorreoSeguimientoResponse> obtenerSeguimientoCorreo(@PathVariable UUID uuid) {
        return ResponseEntity.ok(documentoApplicationService.obtenerSeguimientoCorreo(uuid));
    }

    @GetMapping("/{uuid}/correos")
    public ResponseEntity<DocumentoCorreoSeguimientoResponse> obtenerSeguimientoCorreos(@PathVariable UUID uuid) {
        return ResponseEntity.ok(documentoApplicationService.obtenerSeguimientoCorreo(uuid));
    }

    @PostMapping("/{uuid}/reenviar-correo")
    public ResponseEntity<DocumentoCorreoReenvioResponse> reenviarCorreo(@PathVariable UUID uuid) {
        return ResponseEntity.ok(documentoApplicationService.reenviarCorreo(uuid));
    }

    @PostMapping("/{uuid}/reprocesar")
    public ResponseEntity<DocumentoOperacionManualResponse> reprocesar(
            @PathVariable UUID uuid,
            @RequestBody(required = false) DocumentoReprocesarRequest request
    ) {
        return ResponseEntity.accepted().body(
                documentoApplicationService.reprocesar(uuid, request != null ? request.motivo() : null)
        );
    }

    @PostMapping(path = "/{uuid}/xml-sin-firmar", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<DocumentoOperacionManualResponse> cargarXmlSinFirmar(
            @PathVariable UUID uuid,
            @RequestPart("xml") MultipartFile xmlFile,
            @RequestParam(required = false) String motivo
    ) {
        return ResponseEntity.accepted().body(
                documentoApplicationService.cargarXmlSinFirmar(uuid, xmlFile, motivo)
        );
    }

    @PostMapping("/{uuid}/regenerar-ride")
    public ResponseEntity<DocumentoOperacionManualResponse> regenerarRide(
            @PathVariable UUID uuid,
            @RequestBody(required = false) DocumentoReprocesarRequest request
    ) {
        return ResponseEntity.accepted().body(
                documentoApplicationService.regenerarRide(uuid, request != null ? request.motivo() : null)
        );
    }

    @GetMapping("/autorizacion")
    public ResponseEntity<DocumentoAutorizacionConsultaResponse> consultarAutorizacionPorClave(
            @RequestParam String claveAcceso,
            @RequestParam(defaultValue = "false") boolean incluirXml
    ) {
        return ResponseEntity.ok(documentoApplicationService.consultarAutorizacionPorClave(claveAcceso, incluirXml));
    }

    @PostMapping("/{uuid}/consultar-autorizacion")
    public ResponseEntity<DocumentoAutorizacionManualResponse> consultarAutorizacion(@PathVariable UUID uuid) {
        return ResponseEntity.ok(documentoApplicationService.consultarAutorizacion(uuid));
    }

    @GetMapping
    public ResponseEntity<DocumentoListadoResponse> listarDocumentos(
            @RequestParam(required = false) String empresaUuid,
            @RequestParam(required = false) String tipoDocumento,
            @RequestParam(required = false) String estado,
            @RequestParam(required = false) String busqueda,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        return ResponseEntity.ok(documentoApplicationService.listar(empresaUuid, tipoDocumento, estado, busqueda, page, size));
    }

    @GetMapping("/search")
    public ResponseEntity<DocumentoListadoResponse> buscarDocumentos(
            @RequestParam("q") String q,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        return ResponseEntity.ok(documentoApplicationService.buscarRapido(q, page, size));
    }

    @GetMapping("/export")
    public ResponseEntity<byte[]> exportarDocumentos(
            @RequestParam(required = false) String empresaUuid,
            @RequestParam(required = false) String tipoDocumento,
            @RequestParam(required = false) String estado,
            @RequestParam(required = false) String busqueda
    ) {
        byte[] contenido = documentoApplicationService.exportarCsv(empresaUuid, tipoDocumento, estado, busqueda);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"documentos.csv\"")
                .contentType(MediaType.parseMediaType("text/csv"))
                .body(contenido);
    }

    @GetMapping("/resumen")
    public ResponseEntity<DocumentoResumenOperativoResponse> obtenerResumenOperativo() {
        return ResponseEntity.ok(documentoApplicationService.obtenerResumenOperativo());
    }

    @GetMapping("/contratos/{tipoDocumento}")
    public ResponseEntity<DocumentoContratoResponse> obtenerContrato(@PathVariable String tipoDocumento) {
        return ResponseEntity.ok(documentoContratoService.obtener(tipoDocumento));
    }

    private ResponseEntity<byte[]> descargar(UUID uuid, com.erp.sri_files.domain.documento.DocumentoArchivoTipo tipoArchivo) {
        byte[] contenido = switch (tipoArchivo) {
            case XML_GENERADO -> documentoApplicationService.descargarXml(uuid);
            case XML_FIRMADO -> documentoApplicationService.descargarXmlFirmado(uuid);
            case XML_AUTORIZADO -> documentoApplicationService.descargarXmlAutorizado(uuid);
            case RIDE_PDF -> documentoApplicationService.descargarRide(uuid);
            default -> throw new IllegalArgumentException("Tipo de archivo no soportado para descarga");
        };

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + documentoApplicationService.nombreArchivo(uuid, tipoArchivo) + "\"")
                .contentType(MediaType.parseMediaType(documentoApplicationService.mimeType(uuid, tipoArchivo)))
                .body(contenido);
    }
}
