package com.erp.sri_files.controller;

import com.erp.sri_files.dto.request.PlantillaRideActualizarRequest;
import com.erp.sri_files.dto.request.PlantillaRideCrearRequest;
import com.erp.sri_files.dto.request.PlantillaRideEstadoRequest;
import com.erp.sri_files.dto.response.PlantillaRideResponse;
import com.erp.sri_files.dto.response.VerificacionPlantillaRideResponse;
import com.erp.sri_files.domain.documento.TipoDocumento;
import com.erp.sri_files.service.AuthService;
import com.erp.sri_files.service.DocumentoApplicationService;
import com.erp.sri_files.service.PlantillaRideAdminService;
import com.erp.sri_files.service.RideTemplateCatalogService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1")
public class PlantillaRideController {

    private final PlantillaRideAdminService plantillaRideAdminService;
    private final AuthService authService;
    private final RideTemplateCatalogService rideTemplateCatalogService;
    private final DocumentoApplicationService documentoApplicationService;

    public PlantillaRideController(
            PlantillaRideAdminService plantillaRideAdminService,
            AuthService authService,
            RideTemplateCatalogService rideTemplateCatalogService,
            DocumentoApplicationService documentoApplicationService
    ) {
        this.plantillaRideAdminService = plantillaRideAdminService;
        this.authService = authService;
        this.rideTemplateCatalogService = rideTemplateCatalogService;
        this.documentoApplicationService = documentoApplicationService;
    }

    @GetMapping("/empresas/{empresaId}/plantillas-ride")
    public ResponseEntity<List<PlantillaRideResponse>> listar(@PathVariable UUID empresaId) {
        return ResponseEntity.ok(plantillaRideAdminService.listar(empresaId));
    }

    @PostMapping("/plantillas-ride/{uuid}/verificar")
    public ResponseEntity<VerificacionPlantillaRideResponse> verificar(@PathVariable UUID uuid) {
        return ResponseEntity.ok(plantillaRideAdminService.verificar(uuid));
    }

    @GetMapping("/plantillas-ride/base/{tipoDocumento}")
    public ResponseEntity<byte[]> descargarBase(@PathVariable TipoDocumento tipoDocumento) {
        byte[] contenido = rideTemplateCatalogService.descargarBase(tipoDocumento);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + rideTemplateCatalogService.nombreArchivo(tipoDocumento) + "\"")
                .contentType(MediaType.parseMediaType("application/xml"))
                .body(contenido);
    }

    @GetMapping("/plantillas-ride/{uuid}/preview/{documentoUuid}")
    public ResponseEntity<byte[]> preview(
            @PathVariable UUID uuid,
            @PathVariable UUID documentoUuid
    ) {
        byte[] contenido = documentoApplicationService.previewPlantillaRide(uuid, documentoUuid);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"ride-preview.pdf\"")
                .contentType(MediaType.APPLICATION_PDF)
                .body(contenido);
    }

    @PostMapping(value = "/empresas/{empresaId}/plantillas-ride", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<PlantillaRideResponse> crear(
            @PathVariable UUID empresaId,
            @RequestHeader("Authorization") String authorization,
            @RequestPart("data") @Valid PlantillaRideCrearRequest request,
            @RequestPart(value = "file", required = false) MultipartFile file
    ) {
        return ResponseEntity.status(HttpStatus.CREATED).body(plantillaRideAdminService.crear(
                empresaId,
                request,
                file,
                authService.obtenerUsuarioDesdeToken(authorization.substring("Bearer ".length()).trim())
        ));
    }

    @PutMapping(value = "/plantillas-ride/{uuid}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<PlantillaRideResponse> actualizar(
            @PathVariable UUID uuid,
            @RequestHeader("Authorization") String authorization,
            @RequestPart("data") @Valid PlantillaRideActualizarRequest request,
            @RequestPart(value = "file", required = false) MultipartFile file
    ) {
        return ResponseEntity.ok(plantillaRideAdminService.actualizar(
                uuid,
                request,
                file,
                authService.obtenerUsuarioDesdeToken(authorization.substring("Bearer ".length()).trim())
        ));
    }

    @PatchMapping("/plantillas-ride/{uuid}/estado")
    public ResponseEntity<PlantillaRideResponse> actualizarEstado(
            @PathVariable UUID uuid,
            @RequestHeader("Authorization") String authorization,
            @Valid @RequestBody PlantillaRideEstadoRequest request
    ) {
        return ResponseEntity.ok(plantillaRideAdminService.actualizarEstado(
                uuid,
                request,
                authService.obtenerUsuarioDesdeToken(authorization.substring("Bearer ".length()).trim())
        ));
    }
}
