package com.erp.sri_files.controller;

import com.erp.sri_files.dto.request.EmpresaConfiguracionRequest;
import com.erp.sri_files.dto.request.EmpresaEstadoRequest;
import com.erp.sri_files.dto.request.EmpresaRequest;
import com.erp.sri_files.dto.response.EmpresaConfiguracionResponse;
import com.erp.sri_files.dto.response.EmpresaResponse;
import com.erp.sri_files.exceptions.GlobalExceptionHandler;
import com.erp.sri_files.service.EmpresaService;
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
class EmpresaControllerTest {

    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Mock
    private EmpresaService empresaService;

    @BeforeEach
    void setUp() {
        EmpresaController controller = new EmpresaController(empresaService);
        LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .setMessageConverters(new MappingJackson2HttpMessageConverter(objectMapper))
                .setValidator(validator)
                .build();
    }

    @Test
    void listaEmpresas() throws Exception {
        when(empresaService.listar()).thenReturn(List.of(
                new EmpresaResponse("uuid-1", "0460028810001", "Empresa Demo", "EPMAPA-T", "Tulcan", true, null, 1, "facturacion@demo.ec", false, true)
        ));

        mockMvc.perform(get("/api/v1/empresas"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].ruc").value("0460028810001"));
    }

    @Test
    void obtieneEmpresa() throws Exception {
        UUID uuid = UUID.randomUUID();
        when(empresaService.obtener(uuid))
                .thenReturn(new EmpresaResponse(uuid.toString(), "0460028810001", "Empresa Demo", "EPMAPA-T", "Tulcan", true, null, 1, "facturacion@demo.ec", false, true));

        mockMvc.perform(get("/api/v1/empresas/{uuid}", uuid))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(uuid.toString()));
    }

    @Test
    void creaEmpresa() throws Exception {
        EmpresaRequest request = new EmpresaRequest("0460028810001", "Empresa Demo", "EPMAPA-T", "Tulcan", true, "");

        when(empresaService.crear(any()))
                .thenReturn(new EmpresaResponse("uuid-1", "0460028810001", "Empresa Demo", "EPMAPA-T", "Tulcan", true, null, 1, "facturacion@demo.ec", false, true));

        mockMvc.perform(post("/api/v1/empresas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.razonSocial").value("Empresa Demo"));
    }

    @Test
    void actualizaEmpresa() throws Exception {
        UUID uuid = UUID.randomUUID();
        EmpresaRequest request = new EmpresaRequest("0460028810001", "Empresa Actualizada", "EPMAPA-T", "Tulcan", false, "123");

        when(empresaService.actualizar(eq(uuid), any()))
                .thenReturn(new EmpresaResponse(uuid.toString(), "0460028810001", "Empresa Actualizada", "EPMAPA-T", "Tulcan", false, "123", 2, "facturacion@demo.ec", true, true));

        mockMvc.perform(put("/api/v1/empresas/{uuid}", uuid)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.obligadoContabilidad").value(false));
    }

    @Test
    void actualizaEstadoEmpresa() throws Exception {
        UUID uuid = UUID.randomUUID();

        when(empresaService.actualizarEstado(eq(uuid), any()))
                .thenReturn(new EmpresaResponse(uuid.toString(), "0460028810001", "Empresa Demo", "EPMAPA-T", "Tulcan", true, null, 1, "facturacion@demo.ec", false, false));

        mockMvc.perform(patch("/api/v1/empresas/{uuid}/estado", uuid)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new EmpresaEstadoRequest(false))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.activo").value(false));
    }

    @Test
    void obtieneConfiguracionEmpresa() throws Exception {
        UUID uuid = UUID.randomUUID();
        when(empresaService.obtenerConfiguracion(uuid))
                .thenReturn(new EmpresaConfiguracionResponse(
                        uuid.toString(),
                        2,
                        "facturacion@epmapa.ec",
                        "noreply@epmapa.ec",
                        true,
                        "firma.p12",
                        "firma",
                        "CN=EPMAPA-T",
                        "2026-01-01T00:00:00Z",
                        "2027-01-01T00:00:00Z"
                ));

        mockMvc.perform(get("/api/v1/empresas/{uuid}/configuracion", uuid))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ambienteSri").value(2))
                .andExpect(jsonPath("$.certificadoConfigurado").value(true));
    }

    @Test
    void actualizaConfiguracionEmpresa() throws Exception {
        UUID uuid = UUID.randomUUID();
        EmpresaConfiguracionRequest request = new EmpresaConfiguracionRequest(
                2,
                "facturacion@epmapa.ec",
                "noreply@epmapa.ec",
                "firma.p12",
                "ZmFrZS1iYXNlNjQ=",
                "secreta",
                false
        );

        when(empresaService.actualizarConfiguracion(eq(uuid), any()))
                .thenReturn(new EmpresaConfiguracionResponse(
                        uuid.toString(),
                        2,
                        "facturacion@epmapa.ec",
                        "noreply@epmapa.ec",
                        true,
                        "firma.p12",
                        "firma",
                        "CN=EPMAPA-T",
                        "2026-01-01T00:00:00Z",
                        "2027-01-01T00:00:00Z"
                ));

        mockMvc.perform(put("/api/v1/empresas/{uuid}/configuracion", uuid)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.correoNotificaciones").value("facturacion@epmapa.ec"))
                .andExpect(jsonPath("$.certificadoNombre").value("firma.p12"));
    }
}
