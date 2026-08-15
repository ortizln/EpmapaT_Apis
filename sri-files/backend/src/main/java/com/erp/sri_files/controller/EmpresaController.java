package com.erp.sri_files.controller;

import com.erp.sri_files.dto.request.EmpresaConfiguracionRequest;
import com.erp.sri_files.dto.request.EmpresaEstadoRequest;
import com.erp.sri_files.dto.request.EmpresaRequest;
import com.erp.sri_files.dto.response.EmpresaConfiguracionResponse;
import com.erp.sri_files.dto.response.EmpresaListadoResponse;
import com.erp.sri_files.dto.response.EmpresaResponse;
import com.erp.sri_files.service.EmpresaService;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/empresas")
public class EmpresaController {

    private final EmpresaService empresaService;

    public EmpresaController(EmpresaService empresaService) {
        this.empresaService = empresaService;
    }

    @GetMapping
    public ResponseEntity<EmpresaListadoResponse> listar(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        return ResponseEntity.ok(empresaService.listar(page, size));
    }

    @GetMapping("/{uuid}")
    public ResponseEntity<EmpresaResponse> obtener(@PathVariable UUID uuid) {
        return ResponseEntity.ok(empresaService.obtener(uuid));
    }

    @GetMapping("/{uuid}/configuracion")
    public ResponseEntity<EmpresaConfiguracionResponse> obtenerConfiguracion(@PathVariable UUID uuid) {
        return ResponseEntity.ok(empresaService.obtenerConfiguracion(uuid));
    }

    @PostMapping
    public ResponseEntity<EmpresaResponse> crear(@Valid @RequestBody EmpresaRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(empresaService.crear(request));
    }

    @PutMapping("/{uuid}")
    public ResponseEntity<EmpresaResponse> actualizar(@PathVariable UUID uuid, @Valid @RequestBody EmpresaRequest request) {
        return ResponseEntity.ok(empresaService.actualizar(uuid, request));
    }

    @PatchMapping("/{uuid}/estado")
    public ResponseEntity<EmpresaResponse> actualizarEstado(
            @PathVariable UUID uuid,
            @RequestBody EmpresaEstadoRequest request
    ) {
        return ResponseEntity.ok(empresaService.actualizarEstado(uuid, request));
    }

    @PutMapping("/{uuid}/configuracion")
    public ResponseEntity<EmpresaConfiguracionResponse> actualizarConfiguracion(
            @PathVariable UUID uuid,
            @Valid @RequestBody EmpresaConfiguracionRequest request
    ) {
        return ResponseEntity.ok(empresaService.actualizarConfiguracion(uuid, request));
    }
}
