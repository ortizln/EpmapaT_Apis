package com.erp.sri_files.controller;

import com.erp.sri_files.domain.documento.RecursoEmpresaTipo;
import com.erp.sri_files.dto.request.RecursoEmpresaEstadoRequest;
import com.erp.sri_files.dto.response.RecursoEmpresaResponse;
import com.erp.sri_files.service.AuthService;
import com.erp.sri_files.service.RecursoEmpresaService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1")
public class RecursoEmpresaController {

    private final RecursoEmpresaService recursoEmpresaService;
    private final AuthService authService;

    public RecursoEmpresaController(RecursoEmpresaService recursoEmpresaService, AuthService authService) {
        this.recursoEmpresaService = recursoEmpresaService;
        this.authService = authService;
    }

    @GetMapping("/empresas/{empresaId}/recursos")
    public ResponseEntity<List<RecursoEmpresaResponse>> listar(@PathVariable UUID empresaId) {
        return ResponseEntity.ok(recursoEmpresaService.listar(empresaId));
    }

    @PostMapping(value = "/empresas/{empresaId}/recursos", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<RecursoEmpresaResponse> crear(
            @PathVariable UUID empresaId,
            @RequestHeader("Authorization") String authorization,
            @RequestParam("tipo") RecursoEmpresaTipo tipo,
            @RequestParam(value = "nombre", required = false) String nombre,
            @RequestParam("file") MultipartFile file
    ) {
        return ResponseEntity.status(HttpStatus.CREATED).body(recursoEmpresaService.crear(
                empresaId,
                tipo,
                nombre,
                file,
                authService.obtenerUsuarioDesdeToken(authorization.substring("Bearer ".length()).trim())
        ));
    }

    @PatchMapping("/recursos/{uuid}/estado")
    public ResponseEntity<RecursoEmpresaResponse> actualizarEstado(
            @PathVariable UUID uuid,
            @RequestHeader("Authorization") String authorization,
            @Valid @RequestBody RecursoEmpresaEstadoRequest request
    ) {
        return ResponseEntity.ok(recursoEmpresaService.actualizarEstado(
                uuid,
                request,
                authService.obtenerUsuarioDesdeToken(authorization.substring("Bearer ".length()).trim())
        ));
    }
}
