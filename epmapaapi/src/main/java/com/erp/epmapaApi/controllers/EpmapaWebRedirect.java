package com.erp.epmapaApi.controllers;

import com.erp.epmapaApi.services.FacturaService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/epmapaweb")
public class EpmapaWebRedirect {
    private final FacturaService facturaService;

    @GetMapping("/sincobrar")
    public ResponseEntity<Object> getFacturasSinCobro(@RequestParam Long cuenta) throws Exception {
        Object datos = facturaService.findFacturasSinCobro(cuenta);
        if (datos == null) {
            return ResponseEntity.ok(Map.of("mensaje", "factura no encontradas"));
        }
        return ResponseEntity.ok(datos);
    }
}
