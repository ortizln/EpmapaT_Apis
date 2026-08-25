package com.erp.sri_files.controller;

import com.erp.sri_files.dto.request.CatalogoActivoRequest;
import com.erp.sri_files.dto.request.ClienteCatalogoRequest;
import com.erp.sri_files.dto.request.FormaPagoCatalogoRequest;
import com.erp.sri_files.dto.request.IvaTarifaCatalogoRequest;
import com.erp.sri_files.dto.request.ProductoCatalogoRequest;
import com.erp.sri_files.dto.response.ClienteCatalogoResponse;
import com.erp.sri_files.dto.response.FormaPagoCatalogoResponse;
import com.erp.sri_files.dto.response.IvaTarifaCatalogoResponse;
import com.erp.sri_files.dto.response.ProductoCatalogoResponse;
import com.erp.sri_files.service.CatalogoComercialService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/catalogos-comerciales")
public class CatalogoComercialController {

    private final CatalogoComercialService catalogoComercialService;

    public CatalogoComercialController(CatalogoComercialService catalogoComercialService) {
        this.catalogoComercialService = catalogoComercialService;
    }

    @GetMapping("/clientes")
    public ResponseEntity<List<ClienteCatalogoResponse>> listarClientes(@RequestParam UUID empresaId) {
        return ResponseEntity.ok(catalogoComercialService.listarClientes(empresaId));
    }

    @GetMapping("/clientes/{uuid}")
    public ResponseEntity<ClienteCatalogoResponse> obtenerCliente(@PathVariable UUID uuid) {
        return ResponseEntity.ok(catalogoComercialService.obtenerCliente(uuid));
    }

    @PostMapping("/clientes")
    public ResponseEntity<ClienteCatalogoResponse> crearCliente(@Valid @RequestBody ClienteCatalogoRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(catalogoComercialService.crearCliente(request));
    }

    @PutMapping("/clientes/{uuid}")
    public ResponseEntity<ClienteCatalogoResponse> actualizarCliente(@PathVariable UUID uuid, @Valid @RequestBody ClienteCatalogoRequest request) {
        return ResponseEntity.ok(catalogoComercialService.actualizarCliente(uuid, request));
    }

    @PatchMapping("/clientes/{uuid}/estado")
    public ResponseEntity<ClienteCatalogoResponse> actualizarEstadoCliente(@PathVariable UUID uuid, @RequestBody CatalogoActivoRequest request) {
        return ResponseEntity.ok(catalogoComercialService.actualizarEstadoCliente(uuid, request));
    }

    @GetMapping("/productos")
    public ResponseEntity<List<ProductoCatalogoResponse>> listarProductos(@RequestParam UUID empresaId) {
        return ResponseEntity.ok(catalogoComercialService.listarProductos(empresaId));
    }

    @GetMapping("/productos/{uuid}")
    public ResponseEntity<ProductoCatalogoResponse> obtenerProducto(@PathVariable UUID uuid) {
        return ResponseEntity.ok(catalogoComercialService.obtenerProducto(uuid));
    }

    @PostMapping("/productos")
    public ResponseEntity<ProductoCatalogoResponse> crearProducto(@Valid @RequestBody ProductoCatalogoRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(catalogoComercialService.crearProducto(request));
    }

    @PutMapping("/productos/{uuid}")
    public ResponseEntity<ProductoCatalogoResponse> actualizarProducto(@PathVariable UUID uuid, @Valid @RequestBody ProductoCatalogoRequest request) {
        return ResponseEntity.ok(catalogoComercialService.actualizarProducto(uuid, request));
    }

    @PatchMapping("/productos/{uuid}/estado")
    public ResponseEntity<ProductoCatalogoResponse> actualizarEstadoProducto(@PathVariable UUID uuid, @RequestBody CatalogoActivoRequest request) {
        return ResponseEntity.ok(catalogoComercialService.actualizarEstadoProducto(uuid, request));
    }

    @GetMapping("/formas-pago")
    public ResponseEntity<List<FormaPagoCatalogoResponse>> listarFormasPago(@RequestParam UUID empresaId) {
        return ResponseEntity.ok(catalogoComercialService.listarFormasPago(empresaId));
    }

    @GetMapping("/formas-pago/{uuid}")
    public ResponseEntity<FormaPagoCatalogoResponse> obtenerFormaPago(@PathVariable UUID uuid) {
        return ResponseEntity.ok(catalogoComercialService.obtenerFormaPago(uuid));
    }

    @PostMapping("/formas-pago")
    public ResponseEntity<FormaPagoCatalogoResponse> crearFormaPago(@Valid @RequestBody FormaPagoCatalogoRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(catalogoComercialService.crearFormaPago(request));
    }

    @PutMapping("/formas-pago/{uuid}")
    public ResponseEntity<FormaPagoCatalogoResponse> actualizarFormaPago(@PathVariable UUID uuid, @Valid @RequestBody FormaPagoCatalogoRequest request) {
        return ResponseEntity.ok(catalogoComercialService.actualizarFormaPago(uuid, request));
    }

    @PatchMapping("/formas-pago/{uuid}/estado")
    public ResponseEntity<FormaPagoCatalogoResponse> actualizarEstadoFormaPago(@PathVariable UUID uuid, @RequestBody CatalogoActivoRequest request) {
        return ResponseEntity.ok(catalogoComercialService.actualizarEstadoFormaPago(uuid, request));
    }

    @GetMapping("/iva")
    public ResponseEntity<List<IvaTarifaCatalogoResponse>> listarIva(@RequestParam UUID empresaId) {
        return ResponseEntity.ok(catalogoComercialService.listarIvaTarifas(empresaId));
    }

    @GetMapping("/iva/{uuid}")
    public ResponseEntity<IvaTarifaCatalogoResponse> obtenerIva(@PathVariable UUID uuid) {
        return ResponseEntity.ok(catalogoComercialService.obtenerIvaTarifa(uuid));
    }

    @PostMapping("/iva")
    public ResponseEntity<IvaTarifaCatalogoResponse> crearIva(@Valid @RequestBody IvaTarifaCatalogoRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(catalogoComercialService.crearIvaTarifa(request));
    }

    @PutMapping("/iva/{uuid}")
    public ResponseEntity<IvaTarifaCatalogoResponse> actualizarIva(@PathVariable UUID uuid, @Valid @RequestBody IvaTarifaCatalogoRequest request) {
        return ResponseEntity.ok(catalogoComercialService.actualizarIvaTarifa(uuid, request));
    }

    @PatchMapping("/iva/{uuid}/estado")
    public ResponseEntity<IvaTarifaCatalogoResponse> actualizarEstadoIva(@PathVariable UUID uuid, @RequestBody CatalogoActivoRequest request) {
        return ResponseEntity.ok(catalogoComercialService.actualizarEstadoIvaTarifa(uuid, request));
    }
}
