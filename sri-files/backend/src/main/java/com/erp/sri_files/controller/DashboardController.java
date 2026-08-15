package com.erp.sri_files.controller;

import com.erp.sri_files.dto.response.DashboardDocumentoDiaResponse;
import com.erp.sri_files.dto.response.DashboardDocumentoEstadoResponse;
import com.erp.sri_files.dto.response.DashboardDocumentoTipoResponse;
import com.erp.sri_files.dto.response.DashboardErrorEtapaResponse;
import com.erp.sri_files.dto.response.DashboardResumenResponse;
import com.erp.sri_files.dto.response.DashboardTiemposResponse;
import com.erp.sri_files.service.DashboardService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@RestController
@RequestMapping("/api/v1/dashboard")
public class DashboardController {

    private final DashboardService dashboardService;

    public DashboardController(DashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    @GetMapping("/resumen")
    public ResponseEntity<DashboardResumenResponse> obtenerResumen(@RequestParam(required = false) String empresaUuid) {
        return ResponseEntity.ok(dashboardService.obtenerResumen(empresaUuid));
    }

    @GetMapping("/documentos-por-tipo")
    public ResponseEntity<List<DashboardDocumentoTipoResponse>> obtenerDocumentosPorTipo(@RequestParam(required = false) String empresaUuid) {
        return ResponseEntity.ok(dashboardService.obtenerDocumentosPorTipo(empresaUuid));
    }

    @GetMapping("/documentos-por-estado")
    public ResponseEntity<List<DashboardDocumentoEstadoResponse>> obtenerDocumentosPorEstado(@RequestParam(required = false) String empresaUuid) {
        return ResponseEntity.ok(dashboardService.obtenerDocumentosPorEstado(empresaUuid));
    }

    @GetMapping("/documentos-por-dia")
    public ResponseEntity<List<DashboardDocumentoDiaResponse>> obtenerDocumentosPorDia(@RequestParam(required = false) String empresaUuid) {
        return ResponseEntity.ok(dashboardService.obtenerDocumentosPorDia(empresaUuid));
    }

    @GetMapping("/errores-por-etapa")
    public ResponseEntity<List<DashboardErrorEtapaResponse>> obtenerErroresPorEtapa(@RequestParam(required = false) String empresaUuid) {
        return ResponseEntity.ok(dashboardService.obtenerErroresPorEtapa(empresaUuid));
    }

    @GetMapping("/tiempos")
    public ResponseEntity<DashboardTiemposResponse> obtenerTiempos(@RequestParam(required = false) String empresaUuid) {
        return ResponseEntity.ok(dashboardService.obtenerTiempos(empresaUuid));
    }
}
