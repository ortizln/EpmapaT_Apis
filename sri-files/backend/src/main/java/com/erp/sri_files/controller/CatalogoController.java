package com.erp.sri_files.controller;

import com.erp.sri_files.dto.response.CatalogoItemResponse;
import com.erp.sri_files.service.CatalogoService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/catalogos")
public class CatalogoController {

    private final CatalogoService catalogoService;

    public CatalogoController(CatalogoService catalogoService) {
        this.catalogoService = catalogoService;
    }

    @GetMapping("/tipos-documento")
    public ResponseEntity<List<CatalogoItemResponse>> tiposDocumento() {
        return ResponseEntity.ok(catalogoService.tiposDocumento());
    }

    @GetMapping("/estados-documento")
    public ResponseEntity<List<CatalogoItemResponse>> estadosDocumento() {
        return ResponseEntity.ok(catalogoService.estadosDocumento());
    }

    @GetMapping("/tipos-identificacion")
    public ResponseEntity<List<CatalogoItemResponse>> tiposIdentificacion() {
        return ResponseEntity.ok(catalogoService.tiposIdentificacion());
    }

    @GetMapping("/formas-pago")
    public ResponseEntity<List<CatalogoItemResponse>> formasPago() {
        return ResponseEntity.ok(catalogoService.formasPago());
    }

    @GetMapping("/impuestos")
    public ResponseEntity<List<CatalogoItemResponse>> impuestos() {
        return ResponseEntity.ok(catalogoService.impuestos());
    }

    @GetMapping("/codigos-retencion")
    public ResponseEntity<List<CatalogoItemResponse>> codigosRetencion() {
        return ResponseEntity.ok(catalogoService.codigosRetencion());
    }
}
