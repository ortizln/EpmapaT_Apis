package com.erp.epmapaApi.controllers;

import com.erp.epmapaApi.models.Clientes;
import com.erp.epmapaApi.repositories.ClientesR;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/clientes")
@RequiredArgsConstructor
public class ClientesApi {

    private final ClientesR clientesR;

    @GetMapping("/one")
    public ResponseEntity<?> getClienteById(@RequestParam Long idcliente) {
        return clientesR.findById(idcliente)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
}
