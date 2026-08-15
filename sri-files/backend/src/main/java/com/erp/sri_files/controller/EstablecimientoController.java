package com.erp.sri_files.controller;

import com.erp.sri_files.dto.request.EstablecimientoEstadoRequest;
import com.erp.sri_files.dto.request.EstablecimientoRequest;
import com.erp.sri_files.dto.response.EstablecimientoResponse;
import com.erp.sri_files.service.EstablecimientoService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
public class EstablecimientoController {

    private final EstablecimientoService establecimientoService;

    public EstablecimientoController(EstablecimientoService establecimientoService) {
        this.establecimientoService = establecimientoService;
    }

    @GetMapping("/api/v1/empresas/{empresaUuid}/establecimientos")
    public ResponseEntity<List<EstablecimientoResponse>> listarPorEmpresa(@PathVariable UUID empresaUuid) {
        return ResponseEntity.ok(establecimientoService.listarPorEmpresa(empresaUuid));
    }

    @PostMapping("/api/v1/empresas/{empresaUuid}/establecimientos")
    public ResponseEntity<EstablecimientoResponse> crear(
            @PathVariable UUID empresaUuid,
            @Valid @RequestBody EstablecimientoRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED).body(establecimientoService.crear(empresaUuid, request));
    }

    @GetMapping("/api/v1/establecimientos/{uuid}")
    public ResponseEntity<EstablecimientoResponse> obtener(@PathVariable UUID uuid) {
        return ResponseEntity.ok(establecimientoService.obtener(uuid));
    }

    @PutMapping("/api/v1/establecimientos/{uuid}")
    public ResponseEntity<EstablecimientoResponse> actualizar(
            @PathVariable UUID uuid,
            @Valid @RequestBody EstablecimientoRequest request
    ) {
        return ResponseEntity.ok(establecimientoService.actualizar(uuid, request));
    }

    @PatchMapping("/api/v1/establecimientos/{uuid}/estado")
    public ResponseEntity<EstablecimientoResponse> actualizarEstado(
            @PathVariable UUID uuid,
            @RequestBody EstablecimientoEstadoRequest request
    ) {
        return ResponseEntity.ok(establecimientoService.actualizarEstado(uuid, request));
    }
}
