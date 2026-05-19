package com.erp.epmapaApi.controllers;

import com.erp.epmapaApi.repositories.AbonadosR;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/abonados")
@RequiredArgsConstructor
public class AbonadosApi {

    private final AbonadosR abonadosR;

    @GetMapping("/cuentasOfCliente")
    public ResponseEntity<?> getCuentasOfCliente(@RequestParam Long idcliente) {
        var cuentas = abonadosR.findCuentasByCliente(idcliente);
        if (cuentas.isEmpty()) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.ok(cuentas);
    }

    @GetMapping("/resposablePago")
    public ResponseEntity<?> getAbonadosByResponsablePago(@RequestParam Long idcliente) {
        var cuentas = abonadosR.findByResponsable(idcliente);
        if (cuentas.isEmpty()) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.ok(cuentas);
    }
}
