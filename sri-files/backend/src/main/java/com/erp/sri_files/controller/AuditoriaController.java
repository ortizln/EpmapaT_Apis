package com.erp.sri_files.controller;

import com.erp.sri_files.dto.response.AuditoriaItemResponse;
import com.erp.sri_files.dto.response.AuditoriaListadoResponse;
import com.erp.sri_files.service.AuditoriaService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auditoria")
public class AuditoriaController {

    private final AuditoriaService auditoriaService;

    public AuditoriaController(AuditoriaService auditoriaService) {
        this.auditoriaService = auditoriaService;
    }

    @GetMapping
    public ResponseEntity<AuditoriaListadoResponse> listar(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return ResponseEntity.ok(auditoriaService.listar(page, size));
    }

    @GetMapping("/{id}")
    public ResponseEntity<AuditoriaItemResponse> obtener(@PathVariable String id) {
        return ResponseEntity.ok(auditoriaService.obtener(id));
    }
}
