package com.erp.sri_files.controller;

import com.erp.sri_files.dto.request.DocumentoRecepcionRequest;
import com.erp.sri_files.dto.response.DocumentoAutorizacionConsultaResponse;
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
import com.erp.sri_files.dto.response.DocumentoResumenOperativoResponse;
import com.erp.sri_files.service.DocumentoApplicationService;
import com.erp.sri_files.service.DocumentoContratoService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

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

    @GetMapping("/{uuid}/correo")
    public ResponseEntity<DocumentoCorreoSeguimientoResponse> obtenerSeguimientoCorreo(@PathVariable UUID uuid) {
        return ResponseEntity.ok(documentoApplicationService.obtenerSeguimientoCorreo(uuid));
    }

    @PostMapping("/{uuid}/reenviar-correo")
    public ResponseEntity<DocumentoCorreoReenvioResponse> reenviarCorreo(@PathVariable UUID uuid) {
        return ResponseEntity.ok(documentoApplicationService.reenviarCorreo(uuid));
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

    @GetMapping("/resumen")
    public ResponseEntity<DocumentoResumenOperativoResponse> obtenerResumenOperativo() {
        return ResponseEntity.ok(documentoApplicationService.obtenerResumenOperativo());
    }

    @GetMapping("/contratos/{tipoDocumento}")
    public ResponseEntity<DocumentoContratoResponse> obtenerContrato(@PathVariable String tipoDocumento) {
        return ResponseEntity.ok(documentoContratoService.obtener(tipoDocumento));
    }
}
