package com.erp.sri_files.controller;

import com.erp.sri_files.dto.request.RolCrearRequest;
import com.erp.sri_files.dto.request.RolUpdateRequest;
import com.erp.sri_files.dto.response.PermisoResponse;
import com.erp.sri_files.dto.response.RolAuditoriaListadoResponse;
import com.erp.sri_files.dto.response.RolAuditoriaResponse;
import com.erp.sri_files.dto.response.RolResponse;
import com.erp.sri_files.service.AccessControlService;
import com.erp.sri_files.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1")
public class AccessControlController {

    private final AccessControlService accessControlService;
    private final AuthService authService;

    public AccessControlController(AccessControlService accessControlService, AuthService authService) {
        this.accessControlService = accessControlService;
        this.authService = authService;
    }

    @GetMapping("/roles")
    public ResponseEntity<List<RolResponse>> listarRoles() {
        return ResponseEntity.ok(accessControlService.listarRoles());
    }

    @GetMapping("/roles/{codigo}")
    public ResponseEntity<RolResponse> obtenerRol(@PathVariable String codigo) {
        return ResponseEntity.ok(accessControlService.obtenerRol(codigo));
    }

    @GetMapping("/permisos")
    public ResponseEntity<List<PermisoResponse>> listarPermisos() {
        return ResponseEntity.ok(accessControlService.listarPermisos());
    }

    @PostMapping("/roles")
    public ResponseEntity<RolResponse> crearRol(
            @RequestHeader("Authorization") String authorization,
            @Valid @RequestBody RolCrearRequest request
    ) {
        return ResponseEntity.ok(accessControlService.crearRol(
                request,
                authService.obtenerUsuarioDesdeToken(authorization.substring("Bearer ".length()).trim())
        ));
    }

    @GetMapping("/roles/auditoria-reciente")
    public ResponseEntity<RolAuditoriaListadoResponse> listarAuditoriaReciente(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        return ResponseEntity.ok(accessControlService.listarAuditoriaReciente(page, size));
    }

    @GetMapping("/roles/{codigo}/auditoria")
    public ResponseEntity<List<RolAuditoriaResponse>> obtenerAuditoria(@PathVariable String codigo) {
        return ResponseEntity.ok(accessControlService.obtenerAuditoria(codigo));
    }

    @PutMapping("/roles/{codigo}")
    public ResponseEntity<RolResponse> actualizarRol(
            @PathVariable String codigo,
            @RequestHeader("Authorization") String authorization,
            @Valid @RequestBody RolUpdateRequest request
    ) {
        return ResponseEntity.ok(accessControlService.actualizarRol(
                codigo,
                request,
                authService.obtenerUsuarioDesdeToken(authorization.substring("Bearer ".length()).trim())
        ));
    }
}
