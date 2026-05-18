package com.erp.epmapaApi.controllers;

import com.erp.epmapaApi.services.FacturaService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/epmapaweb")
public class FacturasApi {
    private final FacturaService facturaService;
    @GetMapping("/sincobrar")
    public ResponseEntity<Object> getFacturasSinCobro(@RequestParam Long cuenta) throws Exception {
        Object datos = facturaService.findFacturasSinCobro( cuenta);
        if(datos == null){
            Map<String, Object> respuesta = new HashMap<>();
            respuesta.put("mensaje","Caja no iniciada");
            return ResponseEntity.ok(respuesta);
        }else{
            return ResponseEntity.ok(datos);

        }
    }
}
