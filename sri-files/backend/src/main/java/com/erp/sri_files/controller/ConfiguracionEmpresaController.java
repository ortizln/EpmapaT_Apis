package com.erp.sri_files.controller;

import com.erp.sri_files.dto.request.CorreoConfiguracionRequest;
import com.erp.sri_files.dto.request.SriConfiguracionRequest;
import com.erp.sri_files.dto.response.CorreoConfiguracionResponse;
import com.erp.sri_files.dto.response.SriConfiguracionResponse;
import com.erp.sri_files.service.AuthService;
import com.erp.sri_files.service.EmpresaConfiguracionService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/empresas")
public class ConfiguracionEmpresaController {

    private final EmpresaConfiguracionService empresaConfiguracionService;
    private final AuthService authService;

    public ConfiguracionEmpresaController(EmpresaConfiguracionService empresaConfiguracionService, AuthService authService) {
        this.empresaConfiguracionService = empresaConfiguracionService;
        this.authService = authService;
    }

    @GetMapping("/{empresaId}/configuracion-sri")
    public ResponseEntity<SriConfiguracionResponse> obtenerSri(@PathVariable UUID empresaId) {
        return ResponseEntity.ok(empresaConfiguracionService.obtenerConfiguracionSri(empresaId));
    }

    @PutMapping("/{empresaId}/configuracion-sri")
    public ResponseEntity<SriConfiguracionResponse> actualizarSri(
            @PathVariable UUID empresaId,
            @RequestHeader("Authorization") String authorization,
            @Valid @RequestBody SriConfiguracionRequest request
    ) {
        return ResponseEntity.ok(empresaConfiguracionService.actualizarConfiguracionSri(
                empresaId,
                request,
                authService.obtenerUsuarioDesdeToken(authorization.substring("Bearer ".length()).trim())
        ));
    }

    @GetMapping("/{empresaId}/configuracion-correo")
    public ResponseEntity<CorreoConfiguracionResponse> obtenerCorreo(@PathVariable UUID empresaId) {
        return ResponseEntity.ok(empresaConfiguracionService.obtenerConfiguracionCorreo(empresaId));
    }

    @PutMapping("/{empresaId}/configuracion-correo")
    public ResponseEntity<CorreoConfiguracionResponse> actualizarCorreo(
            @PathVariable UUID empresaId,
            @RequestHeader("Authorization") String authorization,
            @Valid @RequestBody CorreoConfiguracionRequest request
    ) {
        return ResponseEntity.ok(empresaConfiguracionService.actualizarConfiguracionCorreo(
                empresaId,
                request,
                authService.obtenerUsuarioDesdeToken(authorization.substring("Bearer ".length()).trim())
        ));
    }
}
