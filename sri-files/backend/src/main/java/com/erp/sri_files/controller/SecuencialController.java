package com.erp.sri_files.controller;

import com.erp.sri_files.dto.request.SecuencialRequest;
import com.erp.sri_files.dto.response.SecuencialResponse;
import com.erp.sri_files.service.SecuencialService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
public class SecuencialController {

    private final SecuencialService secuencialService;

    public SecuencialController(SecuencialService secuencialService) {
        this.secuencialService = secuencialService;
    }

    @GetMapping("/api/v1/puntos-emision/{puntoEmisionUuid}/secuenciales")
    public ResponseEntity<List<SecuencialResponse>> listar(@PathVariable UUID puntoEmisionUuid) {
        return ResponseEntity.ok(secuencialService.listarPorPuntoEmision(puntoEmisionUuid));
    }

    @PutMapping("/api/v1/puntos-emision/{puntoEmisionUuid}/secuenciales/{tipoDocumento}")
    public ResponseEntity<SecuencialResponse> actualizar(
            @PathVariable UUID puntoEmisionUuid,
            @PathVariable String tipoDocumento,
            @Valid @RequestBody SecuencialRequest request
    ) {
        return ResponseEntity.ok(secuencialService.actualizar(puntoEmisionUuid, tipoDocumento, request));
    }
}
