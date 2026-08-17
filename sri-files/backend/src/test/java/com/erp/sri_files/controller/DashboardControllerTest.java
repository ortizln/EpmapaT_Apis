package com.erp.sri_files.controller;

import com.erp.sri_files.dto.response.DashboardDocumentoDiaResponse;
import com.erp.sri_files.dto.response.DashboardDocumentoEstadoResponse;
import com.erp.sri_files.dto.response.DashboardDocumentoTipoResponse;
import com.erp.sri_files.dto.response.DashboardErrorEtapaResponse;
import com.erp.sri_files.dto.response.DashboardResumenResponse;
import com.erp.sri_files.dto.response.DashboardTiemposResponse;
import com.erp.sri_files.service.DashboardService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class DashboardControllerTest {

    private MockMvc mockMvc;

    @Mock
    private DashboardService dashboardService;

    @BeforeEach
    void setUp() {
        DashboardController controller = new DashboardController(dashboardService);
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setMessageConverters(new MappingJackson2HttpMessageConverter())
                .build();
    }

    @Test
    void obtieneResumen() throws Exception {
        when(dashboardService.obtenerResumen(null))
                .thenReturn(new DashboardResumenResponse(12, 3, 4, 5, 1, 2, 1));

        mockMvc.perform(get("/api/v1/dashboard/resumen"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(12))
                .andExpect(jsonPath("$.procesando").value(4));
    }

    @Test
    void obtieneDocumentosPorTipo() throws Exception {
        when(dashboardService.obtenerDocumentosPorTipo(null))
                .thenReturn(List.of(
                        new DashboardDocumentoTipoResponse("FACTURA", 8),
                        new DashboardDocumentoTipoResponse("NOTA_CREDITO", 2)
                ));

        mockMvc.perform(get("/api/v1/dashboard/documentos-por-tipo"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].tipo").value("FACTURA"))
                .andExpect(jsonPath("$[0].cantidad").value(8));
    }

    @Test
    void obtieneDocumentosPorEstado() throws Exception {
        when(dashboardService.obtenerDocumentosPorEstado(null))
                .thenReturn(List.of(new DashboardDocumentoEstadoResponse("AUTORIZADO", 5)));

        mockMvc.perform(get("/api/v1/dashboard/documentos-por-estado"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].estado").value("AUTORIZADO"));
    }

    @Test
    void obtieneDocumentosPorDia() throws Exception {
        when(dashboardService.obtenerDocumentosPorDia(null))
                .thenReturn(List.of(new DashboardDocumentoDiaResponse("2026-08-14", 7)));

        mockMvc.perform(get("/api/v1/dashboard/documentos-por-dia"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].fecha").value("2026-08-14"));
    }

    @Test
    void obtieneErroresPorEtapa() throws Exception {
        when(dashboardService.obtenerErroresPorEtapa(null))
                .thenReturn(List.of(new DashboardErrorEtapaResponse("CORREO", 3)));

        mockMvc.perform(get("/api/v1/dashboard/errores-por-etapa"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].etapa").value("CORREO"));
    }

    @Test
    void obtieneTiempos() throws Exception {
        when(dashboardService.obtenerTiempos(null))
                .thenReturn(new DashboardTiemposResponse(3400, 2100));

        mockMvc.perform(get("/api/v1/dashboard/tiempos"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.promedioProcesamientoMs").value(3400))
                .andExpect(jsonPath("$.promedioAutorizacionMs").value(2100));
    }
}
