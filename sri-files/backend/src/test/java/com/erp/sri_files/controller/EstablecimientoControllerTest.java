package com.erp.sri_files.controller;

import com.erp.sri_files.dto.request.EstablecimientoEstadoRequest;
import com.erp.sri_files.dto.request.EstablecimientoRequest;
import com.erp.sri_files.dto.response.EstablecimientoResponse;
import com.erp.sri_files.exceptions.GlobalExceptionHandler;
import com.erp.sri_files.service.EstablecimientoService;
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
class EstablecimientoControllerTest {

    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Mock
    private EstablecimientoService establecimientoService;

    @BeforeEach
    void setUp() {
        EstablecimientoController controller = new EstablecimientoController(establecimientoService);
        LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .setMessageConverters(new MappingJackson2HttpMessageConverter(objectMapper))
                .setValidator(validator)
                .build();
    }

    @Test
    void listaPorEmpresa() throws Exception {
        UUID empresaUuid = UUID.randomUUID();
        when(establecimientoService.listarPorEmpresa(empresaUuid)).thenReturn(List.of(
                new EstablecimientoResponse("est-1", empresaUuid.toString(), "Empresa Demo", "001", "Matriz", "Tulcan", true)
        ));

        mockMvc.perform(get("/api/v1/empresas/{empresaUuid}/establecimientos", empresaUuid))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].codigo").value("001"));
    }

    @Test
    void creaEstablecimiento() throws Exception {
        UUID empresaUuid = UUID.randomUUID();
        EstablecimientoRequest request = new EstablecimientoRequest("001", "Matriz", "Tulcan");

        when(establecimientoService.crear(eq(empresaUuid), any()))
                .thenReturn(new EstablecimientoResponse("est-1", empresaUuid.toString(), "Empresa Demo", "001", "Matriz", "Tulcan", true));

        mockMvc.perform(post("/api/v1/empresas/{empresaUuid}/establecimientos", empresaUuid)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.nombre").value("Matriz"));
    }

    @Test
    void obtieneEstablecimiento() throws Exception {
        UUID uuid = UUID.randomUUID();
        when(establecimientoService.obtener(uuid))
                .thenReturn(new EstablecimientoResponse(uuid.toString(), "emp-1", "Empresa Demo", "001", "Matriz", "Tulcan", true));

        mockMvc.perform(get("/api/v1/establecimientos/{uuid}", uuid))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(uuid.toString()));
    }

    @Test
    void actualizaEstablecimiento() throws Exception {
        UUID uuid = UUID.randomUUID();
        EstablecimientoRequest request = new EstablecimientoRequest("002", "Sucursal Norte", "Av. Principal");

        when(establecimientoService.actualizar(eq(uuid), any()))
                .thenReturn(new EstablecimientoResponse(uuid.toString(), "emp-1", "Empresa Demo", "002", "Sucursal Norte", "Av. Principal", true));

        mockMvc.perform(put("/api/v1/establecimientos/{uuid}", uuid)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.codigo").value("002"));
    }

    @Test
    void actualizaEstadoEstablecimiento() throws Exception {
        UUID uuid = UUID.randomUUID();

        when(establecimientoService.actualizarEstado(eq(uuid), any()))
                .thenReturn(new EstablecimientoResponse(uuid.toString(), "emp-1", "Empresa Demo", "001", "Matriz", "Tulcan", false));

        mockMvc.perform(patch("/api/v1/establecimientos/{uuid}/estado", uuid)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new EstablecimientoEstadoRequest(false))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.activo").value(false));
    }
}
