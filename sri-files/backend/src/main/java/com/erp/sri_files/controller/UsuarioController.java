package com.erp.sri_files.controller;

import com.erp.sri_files.dto.request.UsuarioCrearRequest;
import com.erp.sri_files.dto.request.UsuarioActualizarRequest;
import com.erp.sri_files.dto.request.UsuarioEstadoRequest;
import com.erp.sri_files.dto.request.UsuarioPasswordResetRequest;
import com.erp.sri_files.dto.response.UsuarioAutenticadoResponse;
import com.erp.sri_files.dto.response.UsuarioAuditoriaListadoResponse;
import com.erp.sri_files.dto.response.UsuarioAuditoriaResponse;
import com.erp.sri_files.dto.response.UsuarioSistemaListadoResponse;
import com.erp.sri_files.dto.response.UsuarioSistemaResponse;
import com.erp.sri_files.service.AuthService;
import com.erp.sri_files.service.UsuarioSistemaService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/usuarios")
public class UsuarioController {

    private final UsuarioSistemaService usuarioSistemaService;
    private final AuthService authService;

    public UsuarioController(UsuarioSistemaService usuarioSistemaService, AuthService authService) {
        this.usuarioSistemaService = usuarioSistemaService;
        this.authService = authService;
    }

    @GetMapping
    public ResponseEntity<UsuarioSistemaListadoResponse> listar(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        return ResponseEntity.ok(usuarioSistemaService.listar(page, size));
    }

    @GetMapping("/{uuid}/auditoria")
    public ResponseEntity<List<UsuarioAuditoriaResponse>> obtenerAuditoria(@PathVariable UUID uuid) {
        return ResponseEntity.ok(usuarioSistemaService.obtenerAuditoria(uuid));
    }

    @GetMapping("/auditoria-reciente")
    public ResponseEntity<UsuarioAuditoriaListadoResponse> obtenerAuditoriaReciente(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        return ResponseEntity.ok(usuarioSistemaService.listarAuditoriaReciente(page, size));
    }

    @PostMapping
    public ResponseEntity<UsuarioSistemaResponse> crear(
            @RequestHeader("Authorization") String authorization,
            @Valid @RequestBody UsuarioCrearRequest request
    ) {
        UsuarioAutenticadoResponse actor = authService.obtenerUsuarioDesdeToken(
                authorization.substring("Bearer ".length()).trim()
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(usuarioSistemaService.crear(request, actor));
    }

    @PutMapping("/{uuid}")
    public ResponseEntity<UsuarioSistemaResponse> actualizar(
            @PathVariable UUID uuid,
            @RequestHeader("Authorization") String authorization,
            @Valid @RequestBody UsuarioActualizarRequest request
    ) {
        UsuarioAutenticadoResponse actor = authService.obtenerUsuarioDesdeToken(
                authorization.substring("Bearer ".length()).trim()
        );
        return ResponseEntity.ok(usuarioSistemaService.actualizar(uuid, request, actor));
    }

    @PatchMapping("/{uuid}/estado")
    public ResponseEntity<UsuarioSistemaResponse> actualizarEstado(
            @PathVariable UUID uuid,
            @RequestHeader("Authorization") String authorization,
            @RequestBody UsuarioEstadoRequest request
    ) {
        UsuarioAutenticadoResponse actor = authService.obtenerUsuarioDesdeToken(
                authorization.substring("Bearer ".length()).trim()
        );
        return ResponseEntity.ok(usuarioSistemaService.actualizarEstado(uuid, request, actor));
    }

    @PatchMapping("/{uuid}/password")
    public ResponseEntity<UsuarioSistemaResponse> resetearPassword(
            @PathVariable UUID uuid,
            @RequestHeader("Authorization") String authorization,
            @Valid @RequestBody UsuarioPasswordResetRequest request
    ) {
        UsuarioAutenticadoResponse actor = authService.obtenerUsuarioDesdeToken(
                authorization.substring("Bearer ".length()).trim()
        );
        return ResponseEntity.ok(usuarioSistemaService.resetearPassword(uuid, request, actor));
    }
}
