package com.erp.sri_files.controller;

import com.erp.sri_files.dto.request.PuntoEmisionEstadoRequest;
import com.erp.sri_files.dto.request.PuntoEmisionRequest;
import com.erp.sri_files.dto.response.PuntoEmisionResponse;
import com.erp.sri_files.exceptions.GlobalExceptionHandler;
import com.erp.sri_files.service.PuntoEmisionService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class PuntoEmisionControllerTest {

    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Mock
    private PuntoEmisionService puntoEmisionService;

    @BeforeEach
    void setUp() {
        PuntoEmisionController controller = new PuntoEmisionController(puntoEmisionService);
        LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .setMessageConverters(new MappingJackson2HttpMessageConverter(objectMapper))
                .setValidator(validator)
                .build();
    }

    @Test
    void listaPorEstablecimiento() throws Exception {
        UUID establecimientoUuid = UUID.randomUUID();
        when(puntoEmisionService.listarPorEstablecimiento(establecimientoUuid)).thenReturn(List.of(
                new PuntoEmisionResponse("pto-1", establecimientoUuid.toString(), "001", "emp-1", "Empresa Demo", "001", "Caja principal", true)
        ));

        mockMvc.perform(get("/api/v1/establecimientos/{establecimientoUuid}/puntos-emision", establecimientoUuid))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].codigo").value("001"));
    }

    @Test
    void creaPuntoEmision() throws Exception {
        UUID establecimientoUuid = UUID.randomUUID();
        PuntoEmisionRequest request = new PuntoEmisionRequest("001", "Caja principal");

        when(puntoEmisionService.crear(eq(establecimientoUuid), any()))
                .thenReturn(new PuntoEmisionResponse("pto-1", establecimientoUuid.toString(), "001", "emp-1", "Empresa Demo", "001", "Caja principal", true));

        mockMvc.perform(post("/api/v1/establecimientos/{establecimientoUuid}/puntos-emision", establecimientoUuid)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.nombre").value("Caja principal"));
    }

    @Test
    void obtienePuntoEmision() throws Exception {
        UUID uuid = UUID.randomUUID();
        when(puntoEmisionService.obtener(uuid))
                .thenReturn(new PuntoEmisionResponse(uuid.toString(), "est-1", "001", "emp-1", "Empresa Demo", "001", "Caja principal", true));

        mockMvc.perform(get("/api/v1/puntos-emision/{uuid}", uuid))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(uuid.toString()));
    }

    @Test
    void actualizaPuntoEmision() throws Exception {
        UUID uuid = UUID.randomUUID();
        PuntoEmisionRequest request = new PuntoEmisionRequest("002", "Caja secundaria");

        when(puntoEmisionService.actualizar(eq(uuid), any()))
                .thenReturn(new PuntoEmisionResponse(uuid.toString(), "est-1", "001", "emp-1", "Empresa Demo", "002", "Caja secundaria", true));

        mockMvc.perform(put("/api/v1/puntos-emision/{uuid}", uuid)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.codigo").value("002"));
    }

    @Test
    void actualizaEstadoPuntoEmision() throws Exception {
        UUID uuid = UUID.randomUUID();

        when(puntoEmisionService.actualizarEstado(eq(uuid), any()))
                .thenReturn(new PuntoEmisionResponse(uuid.toString(), "est-1", "001", "emp-1", "Empresa Demo", "001", "Caja principal", false));

        mockMvc.perform(patch("/api/v1/puntos-emision/{uuid}/estado", uuid)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new PuntoEmisionEstadoRequest(false))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.activo").value(false));
    }
}
