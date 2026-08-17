package com.erp.sri_files.controller;

import com.erp.sri_files.dto.response.CorreoPendienteItemResponse;
import com.erp.sri_files.dto.response.CorreoPendienteResponse;
import com.erp.sri_files.dto.response.MonitorComponenteEstadoResponse;
import com.erp.sri_files.dto.response.MonitorHealthResponse;
import com.erp.sri_files.dto.response.MonitorPendienteItemResponse;
import com.erp.sri_files.dto.response.MonitorPendientesResponse;
import com.erp.sri_files.dto.response.MonitorResumenResponse;
import com.erp.sri_files.service.MonitorService;
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
class MonitorControllerTest {

    private MockMvc mockMvc;

    @Mock
    private MonitorService monitorService;

    @BeforeEach
    void setUp() {
        MonitorController controller = new MonitorController(monitorService);
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setMessageConverters(new MappingJackson2HttpMessageConverter())
                .build();
    }

    @Test
    void obtieneHealth() throws Exception {
        when(monitorService.obtenerHealth()).thenReturn(new MonitorHealthResponse(
                "UP",
                "2026-08-15T18:15:00",
                new MonitorResumenResponse(10, 2, 1, 1, 0, 6),
                List.of(
                        new MonitorComponenteEstadoResponse("database", "UP", "Conexion operativa"),
                        new MonitorComponenteEstadoResponse("storage", "UP", "data/sri-files"),
                        new MonitorComponenteEstadoResponse("email-ms", "DOWN", "No fue posible conectar")
                )
        ));

        mockMvc.perform(get("/api/v1/monitoreo/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("UP"))
                .andExpect(jsonPath("$.resumen.totalDocumentos").value(10))
                .andExpect(jsonPath("$.componentes[0].nombre").value("database"));
    }

    @Test
    void obtieneResumen() throws Exception {
        when(monitorService.obtenerResumen()).thenReturn(new MonitorResumenResponse(12, 3, 2, 1, 2, 4));

        mockMvc.perform(get("/api/v1/monitoreo/resumen"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalDocumentos").value(12))
                .andExpect(jsonPath("$.pendientesProcesamiento").value(3))
                .andExpect(jsonPath("$.conError").value(2));
    }

    @Test
    void obtienePendientes() throws Exception {
        when(monitorService.obtenerPendientes()).thenReturn(new MonitorPendientesResponse(
                1,
                List.of(new MonitorPendienteItemResponse(
                        "uuid-1",
                        "FACTURA",
                        "001-001-000000123",
                        "Cliente Demo",
                        "PENDIENTE_AUTORIZACION",
                        "2026-08-15T19:00:00",
                        2,
                        false
                ))
        ));

        mockMvc.perform(get("/api/v1/monitoreo/pendientes"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(1))
                .andExpect(jsonPath("$.items[0].tipoDocumento").value("FACTURA"))
                .andExpect(jsonPath("$.items[0].estado").value("PENDIENTE_AUTORIZACION"));
    }

    @Test
    void obtieneCorreosPendientes() throws Exception {
        when(monitorService.obtenerCorreosPendientes()).thenReturn(new CorreoPendienteResponse(
                1,
                List.of(new CorreoPendienteItemResponse(
                        "uuid-2",
                        "RETENCION",
                        "001-001-000000456",
                        "Proveedor Demo",
                        "correo@demo.com",
                        "CORREO_PENDIENTE",
                        "2026-08-15T18:00:00",
                        "2026-08-15T18:05:00",
                        true
                ))
        ));

        mockMvc.perform(get("/api/v1/monitoreo/correos"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(1))
                .andExpect(jsonPath("$.items[0].destinatario").value("correo@demo.com"))
                .andExpect(jsonPath("$.items[0].requiereIntervencion").value(true));
    }
}
