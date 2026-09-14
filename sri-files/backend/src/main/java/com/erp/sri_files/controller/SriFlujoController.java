package com.erp.sri_files.controller;

import com.erp.sri_files.dto.response.SriFlujoResumenResponse;
import com.erp.sri_files.service.SriFlujoService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Supervisión del flujo SRI (Fase 6): estado del histórico en BD, contadores
 * en memoria y salud del circuit breaker.
 */
@RestController
@RequestMapping("/api/v1/sri-flujo")
public class SriFlujoController {

    private final SriFlujoService sriFlujoService;

    public SriFlujoController(SriFlujoService sriFlujoService) {
        this.sriFlujoService = sriFlujoService;
    }

    @GetMapping("/resumen")
    public ResponseEntity<SriFlujoResumenResponse> resumen() {
        return ResponseEntity.ok(sriFlujoService.resumen());
    }
}