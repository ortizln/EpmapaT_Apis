package com.erp.sri_files.controller;

import com.erp.sri_files.dto.request.EmpresaConfiguracionRequest;
import com.erp.sri_files.dto.request.EmpresaEstadoRequest;
import com.erp.sri_files.dto.request.EmpresaRequest;
import com.erp.sri_files.dto.response.EmpresaAuditoriaListadoItemResponse;
import com.erp.sri_files.dto.response.EmpresaAuditoriaListadoResponse;
import com.erp.sri_files.dto.response.EmpresaAuditoriaResponse;
import com.erp.sri_files.dto.response.EmpresaConfiguracionResponse;
import com.erp.sri_files.dto.response.EmpresaListadoResponse;
import com.erp.sri_files.dto.response.EmpresaResponse;
import com.erp.sri_files.dto.response.UsuarioAutenticadoResponse;
import com.erp.sri_files.exceptions.GlobalExceptionHandler;
import com.erp.sri_files.service.AuthService;
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
    @Mock
    private AuthService authService;

    @BeforeEach
    void setUp() {
        EmpresaController controller = new EmpresaController(empresaService, authService);
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
        when(empresaService.listar(0, 10)).thenReturn(new EmpresaListadoResponse(
                List.of(new EmpresaResponse("uuid-1", "0460028810001", "Empresa Demo", "EPMAPA-T", "Tulcan", true, null, 1, "facturacion@demo.ec", false, true)),
                0,
                10,
                1,
                1
        ));

        mockMvc.perform(get("/api/v1/empresas"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].ruc").value("0460028810001"))
                .andExpect(jsonPath("$.totalItems").value(1));
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

        when(authService.obtenerUsuarioDesdeToken("token-demo"))
                .thenReturn(new UsuarioAutenticadoResponse("1", "Admin", "admin@sri.local", List.of("ADMIN"), List.of("CATALOGO_ADMINISTRAR")));
        when(empresaService.crear(any(), any()))
                .thenReturn(new EmpresaResponse("uuid-1", "0460028810001", "Empresa Demo", "EPMAPA-T", "Tulcan", true, null, 1, "facturacion@demo.ec", false, true));

        mockMvc.perform(post("/api/v1/empresas")
                        .header("Authorization", "Bearer token-demo")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.razonSocial").value("Empresa Demo"));
    }

    @Test
    void actualizaEmpresa() throws Exception {
        UUID uuid = UUID.randomUUID();
        EmpresaRequest request = new EmpresaRequest("0460028810001", "Empresa Actualizada", "EPMAPA-T", "Tulcan", false, "123");

        when(authService.obtenerUsuarioDesdeToken("token-demo"))
                .thenReturn(new UsuarioAutenticadoResponse("1", "Admin", "admin@sri.local", List.of("ADMIN"), List.of("CATALOGO_ADMINISTRAR")));
        when(empresaService.actualizar(eq(uuid), any(), any()))
                .thenReturn(new EmpresaResponse(uuid.toString(), "0460028810001", "Empresa Actualizada", "EPMAPA-T", "Tulcan", false, "123", 2, "facturacion@demo.ec", true, true));

        mockMvc.perform(put("/api/v1/empresas/{uuid}", uuid)
                        .header("Authorization", "Bearer token-demo")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.obligadoContabilidad").value(false));
    }

    @Test
    void actualizaEstadoEmpresa() throws Exception {
        UUID uuid = UUID.randomUUID();

        when(authService.obtenerUsuarioDesdeToken("token-demo"))
                .thenReturn(new UsuarioAutenticadoResponse("1", "Admin", "admin@sri.local", List.of("ADMIN"), List.of("CATALOGO_ADMINISTRAR")));
        when(empresaService.actualizarEstado(eq(uuid), any(), any()))
                .thenReturn(new EmpresaResponse(uuid.toString(), "0460028810001", "Empresa Demo", "EPMAPA-T", "Tulcan", true, null, 1, "facturacion@demo.ec", false, false));

        mockMvc.perform(patch("/api/v1/empresas/{uuid}/estado", uuid)
                        .header("Authorization", "Bearer token-demo")
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

        when(authService.obtenerUsuarioDesdeToken("token-demo"))
                .thenReturn(new UsuarioAutenticadoResponse("1", "Admin", "admin@sri.local", List.of("ADMIN"), List.of("CONFIGURACION_CORREO_ADMINISTRAR")));
        when(empresaService.actualizarConfiguracion(eq(uuid), any(), any()))
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
                        .header("Authorization", "Bearer token-demo")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.correoNotificaciones").value("facturacion@epmapa.ec"))
                .andExpect(jsonPath("$.certificadoNombre").value("firma.p12"));
    }

    @Test
    void obtieneAuditoriaRecienteEmpresas() throws Exception {
        when(empresaService.listarAuditoriaReciente(0, 10)).thenReturn(new EmpresaAuditoriaListadoResponse(
                List.of(new EmpresaAuditoriaListadoItemResponse(
                        1L,
                        "uuid-1",
                        "0460028810001",
                        "Empresa Demo",
                        "EMPRESA_CONFIGURACION_ACTUALIZADA",
                        "Configuracion sensible actualizada",
                        "Admin",
                        "2026-08-17T10:00:00"
                )),
                0,
                10,
                1,
                1
        ));

        mockMvc.perform(get("/api/v1/empresas/auditoria-reciente"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].ruc").value("0460028810001"))
                .andExpect(jsonPath("$.items[0].accion").value("EMPRESA_CONFIGURACION_ACTUALIZADA"));
    }

    @Test
    void obtieneAuditoriaPorEmpresa() throws Exception {
        UUID uuid = UUID.randomUUID();
        when(empresaService.obtenerAuditoria(uuid)).thenReturn(List.of(
                new EmpresaAuditoriaResponse(
                        "EMPRESA_ACTUALIZADA",
                        "Empresa Demo actualizada",
                        "Admin",
                        "2026-08-17T10:00:00"
                )
        ));

        mockMvc.perform(get("/api/v1/empresas/{uuid}/auditoria", uuid))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].accion").value("EMPRESA_ACTUALIZADA"));
    }
}
