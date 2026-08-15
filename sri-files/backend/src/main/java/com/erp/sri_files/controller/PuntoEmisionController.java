package com.erp.sri_files.controller;

import com.erp.sri_files.dto.request.PuntoEmisionEstadoRequest;
import com.erp.sri_files.dto.request.PuntoEmisionRequest;
import com.erp.sri_files.dto.response.PuntoEmisionResponse;
import com.erp.sri_files.service.PuntoEmisionService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
public class PuntoEmisionController {

    private final PuntoEmisionService puntoEmisionService;

    public PuntoEmisionController(PuntoEmisionService puntoEmisionService) {
        this.puntoEmisionService = puntoEmisionService;
    }

    @GetMapping("/api/v1/establecimientos/{establecimientoUuid}/puntos-emision")
    public ResponseEntity<List<PuntoEmisionResponse>> listarPorEstablecimiento(@PathVariable UUID establecimientoUuid) {
        return ResponseEntity.ok(puntoEmisionService.listarPorEstablecimiento(establecimientoUuid));
    }

    @PostMapping("/api/v1/establecimientos/{establecimientoUuid}/puntos-emision")
    public ResponseEntity<PuntoEmisionResponse> crear(
            @PathVariable UUID establecimientoUuid,
            @Valid @RequestBody PuntoEmisionRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED).body(puntoEmisionService.crear(establecimientoUuid, request));
    }

    @GetMapping("/api/v1/puntos-emision/{uuid}")
    public ResponseEntity<PuntoEmisionResponse> obtener(@PathVariable UUID uuid) {
        return ResponseEntity.ok(puntoEmisionService.obtener(uuid));
    }

    @PutMapping("/api/v1/puntos-emision/{uuid}")
    public ResponseEntity<PuntoEmisionResponse> actualizar(
            @PathVariable UUID uuid,
            @Valid @RequestBody PuntoEmisionRequest request
    ) {
        return ResponseEntity.ok(puntoEmisionService.actualizar(uuid, request));
    }

    @PatchMapping("/api/v1/puntos-emision/{uuid}/estado")
    public ResponseEntity<PuntoEmisionResponse> actualizarEstado(
            @PathVariable UUID uuid,
            @RequestBody PuntoEmisionEstadoRequest request
    ) {
        return ResponseEntity.ok(puntoEmisionService.actualizarEstado(uuid, request));
    }
}
