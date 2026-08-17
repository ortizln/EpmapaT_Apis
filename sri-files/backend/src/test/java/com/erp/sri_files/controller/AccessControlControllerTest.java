package com.erp.sri_files.controller;

import com.erp.sri_files.dto.request.RolUpdateRequest;
import com.erp.sri_files.dto.response.PermisoResponse;
import com.erp.sri_files.dto.response.RolAuditoriaListadoItemResponse;
import com.erp.sri_files.dto.response.RolAuditoriaListadoResponse;
import com.erp.sri_files.dto.response.RolAuditoriaResponse;
import com.erp.sri_files.dto.response.RolResponse;
import com.erp.sri_files.dto.response.UsuarioAutenticadoResponse;
import com.erp.sri_files.exceptions.GlobalExceptionHandler;
import com.erp.sri_files.service.AccessControlService;
import com.erp.sri_files.service.AuthService;
import com.fasterxml.jackson.databind.ObjectMapper;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class AccessControlControllerTest {

    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Mock
    private AccessControlService accessControlService;
    @Mock
    private AuthService authService;

    @BeforeEach
    void setUp() {
        AccessControlController controller = new AccessControlController(accessControlService, authService);
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .setMessageConverters(new MappingJackson2HttpMessageConverter(objectMapper))
                .build();
    }

    @Test
    void listaRoles() throws Exception {
        when(accessControlService.listarRoles()).thenReturn(List.of(
                new RolResponse("ADMIN", "Administrador", "Control total", List.of("ROL_VER", "USUARIO_VER"))
        ));

        mockMvc.perform(get("/api/v1/roles"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].codigo").value("ADMIN"))
                .andExpect(jsonPath("$[0].permisos[0]").value("ROL_VER"));
    }

    @Test
    void listaPermisos() throws Exception {
        when(accessControlService.listarPermisos()).thenReturn(List.of(
                new PermisoResponse("ROL_VER", "Ver roles", "Permite consultar la matriz", "Seguridad")
        ));

        mockMvc.perform(get("/api/v1/permisos"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].codigo").value("ROL_VER"))
                .andExpect(jsonPath("$[0].categoria").value("Seguridad"));
    }

    @Test
    void actualizaRol() throws Exception {
        when(accessControlService.actualizarRol(
                org.mockito.ArgumentMatchers.eq("OPERADOR"),
                org.mockito.ArgumentMatchers.any(RolUpdateRequest.class),
                org.mockito.ArgumentMatchers.any(UsuarioAutenticadoResponse.class)
        ))
                .thenReturn(new RolResponse("OPERADOR", "Operador", "Rol operativo ajustado", List.of("DOCUMENTO_VER", "DOCUMENTO_CREAR")));
        when(authService.obtenerUsuarioDesdeToken("token-demo"))
                .thenReturn(new UsuarioAutenticadoResponse("1", "Admin", "admin@sri.local", List.of("ADMIN"), List.of("ROL_ADMINISTRAR")));

        mockMvc.perform(put("/api/v1/roles/OPERADOR")
                        .header("Authorization", "Bearer token-demo")
                        .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "nombre": "Operador",
                                  "descripcion": "Rol operativo ajustado",
                                  "permisos": ["DOCUMENTO_VER", "DOCUMENTO_CREAR"]
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.codigo").value("OPERADOR"))
                .andExpect(jsonPath("$.permisos[1]").value("DOCUMENTO_CREAR"));
    }

    @Test
    void obtieneAuditoriaRecienteRoles() throws Exception {
        when(accessControlService.listarAuditoriaReciente(0, 10)).thenReturn(new RolAuditoriaListadoResponse(
                List.of(new RolAuditoriaListadoItemResponse(
                        1L,
                        "ADMIN",
                        "Administrador",
                        "ROL_ACTUALIZADO",
                        "Rol ADMIN actualizado",
                        "Admin",
                        "2026-08-16T10:00:00"
                )),
                0,
                10,
                1,
                1
        ));

        mockMvc.perform(get("/api/v1/roles/auditoria-reciente"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].rolCodigo").value("ADMIN"))
                .andExpect(jsonPath("$.items[0].accion").value("ROL_ACTUALIZADO"));
    }

    @Test
    void obtieneAuditoriaPorRol() throws Exception {
        when(accessControlService.obtenerAuditoria("ADMIN")).thenReturn(List.of(
                new RolAuditoriaResponse(
                        "ROL_ACTUALIZADO",
                        "Rol ADMIN actualizado",
                        "Admin",
                        "2026-08-16T10:00:00"
                )
        ));

        mockMvc.perform(get("/api/v1/roles/ADMIN/auditoria"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].accion").value("ROL_ACTUALIZADO"));
    }
}
