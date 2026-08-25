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
@RequestMapping("/api/v1/monitoring")
public class MonitoringCompatibilityController {

    private final MonitorService monitorService;

    public MonitoringCompatibilityController(MonitorService monitorService) {
        this.monitorService = monitorService;
    }

    @GetMapping("/status")
    public ResponseEntity<MonitorHealthResponse> obtenerStatus() {
        return ResponseEntity.ok(monitorService.obtenerHealth());
    }

    @GetMapping("/pending")
    public ResponseEntity<MonitorPendientesResponse> obtenerPending() {
        return ResponseEntity.ok(monitorService.obtenerPendientes());
    }

    @GetMapping("/summary")
    public ResponseEntity<MonitorResumenResponse> obtenerSummary() {
        return ResponseEntity.ok(monitorService.obtenerResumen());
    }

    @GetMapping("/emails")
    public ResponseEntity<CorreoPendienteResponse> obtenerEmails() {
        return ResponseEntity.ok(monitorService.obtenerCorreosPendientes());
    }
}
