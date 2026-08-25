package com.erp.sri_files.controller;

import com.erp.sri_files.dto.request.CertificadoEstadoRequest;
import com.erp.sri_files.dto.response.CertificadoResponse;
import com.erp.sri_files.dto.response.VerificacionCertificadoResponse;
import com.erp.sri_files.service.AuthService;
import com.erp.sri_files.service.EmpresaConfiguracionService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1")
public class CertificadoController {

    private final EmpresaConfiguracionService empresaConfiguracionService;
    private final AuthService authService;

    public CertificadoController(EmpresaConfiguracionService empresaConfiguracionService, AuthService authService) {
        this.empresaConfiguracionService = empresaConfiguracionService;
        this.authService = authService;
    }

    @GetMapping("/empresas/{empresaId}/certificados")
    public ResponseEntity<List<CertificadoResponse>> listar(@PathVariable UUID empresaId) {
        return ResponseEntity.ok(empresaConfiguracionService.listarCertificados(empresaId));
    }

    @PostMapping(value = "/empresas/{empresaId}/certificados", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<CertificadoResponse> cargar(
            @PathVariable UUID empresaId,
            @RequestHeader("Authorization") String authorization,
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "nombre", required = false) String nombre,
            @RequestParam("clave") String clave
    ) {
        return ResponseEntity.status(HttpStatus.CREATED).body(empresaConfiguracionService.cargarCertificado(
                empresaId,
                file,
                nombre,
                clave,
                authService.obtenerUsuarioDesdeToken(authorization.substring("Bearer ".length()).trim())
        ));
    }

    @PostMapping("/certificados/{uuid}/verificar")
    public ResponseEntity<VerificacionCertificadoResponse> verificar(@PathVariable UUID uuid) {
        return ResponseEntity.ok(empresaConfiguracionService.verificarCertificado(uuid));
    }

    @PatchMapping("/certificados/{uuid}/estado")
    public ResponseEntity<CertificadoResponse> actualizarEstado(
            @PathVariable UUID uuid,
            @RequestHeader("Authorization") String authorization,
            @Valid @RequestBody CertificadoEstadoRequest request
    ) {
        return ResponseEntity.ok(empresaConfiguracionService.actualizarEstadoCertificado(
                uuid,
                request,
                authService.obtenerUsuarioDesdeToken(authorization.substring("Bearer ".length()).trim())
        ));
    }
}
