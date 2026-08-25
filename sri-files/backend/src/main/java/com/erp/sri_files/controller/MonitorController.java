package com.erp.sri_files.controller;

import com.erp.sri_files.dto.response.CorreoPendienteResponse;
import com.erp.sri_files.dto.response.MonitorHealthResponse;
import com.erp.sri_files.dto.response.MonitorPendientesResponse;
import com.erp.sri_files.dto.response.MonitorResumenResponse;
import com.erp.sri_files.service.MonitorService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/monitoreo")
public class MonitorController {

    private final MonitorService monitorService;

    public MonitorController(MonitorService monitorService) {
        this.monitorService = monitorService;
    }

    @GetMapping("/health")
    public ResponseEntity<MonitorHealthResponse> obtenerHealth() {
        return ResponseEntity.ok(monitorService.obtenerHealth());
    }

    @GetMapping("/resumen")
    public ResponseEntity<MonitorResumenResponse> obtenerResumen() {
        return ResponseEntity.ok(monitorService.obtenerResumen());
    }

    @GetMapping("/pendientes")
    public ResponseEntity<MonitorPendientesResponse> obtenerPendientes() {
        return ResponseEntity.ok(monitorService.obtenerPendientes());
    }

    @GetMapping("/correos")
    public ResponseEntity<CorreoPendienteResponse> obtenerCorreosPendientes() {
        return ResponseEntity.ok(monitorService.obtenerCorreosPendientes());
    }

}
