package com.erp.sri_files.controller;

import com.erp.sri_files.dto.request.UsuarioCrearRequest;
import com.erp.sri_files.dto.request.UsuarioEstadoRequest;
import com.erp.sri_files.dto.request.UsuarioPasswordResetRequest;
import com.erp.sri_files.dto.response.UsuarioAutenticadoResponse;
import com.erp.sri_files.dto.response.UsuarioAuditoriaResponse;
import com.erp.sri_files.dto.response.UsuarioSistemaResponse;
import com.erp.sri_files.exceptions.GlobalExceptionHandler;
import com.erp.sri_files.service.AuthService;
import com.erp.sri_files.service.UsuarioSistemaService;
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
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class UsuarioControllerTest {

    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Mock
    private UsuarioSistemaService usuarioSistemaService;
    @Mock
    private AuthService authService;

    @BeforeEach
    void setUp() {
        UsuarioController controller = new UsuarioController(usuarioSistemaService, authService);
        LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .setMessageConverters(new MappingJackson2HttpMessageConverter(objectMapper))
                .setValidator(validator)
                .build();
    }

    @Test
    void listaUsuarios() throws Exception {
        when(usuarioSistemaService.listar()).thenReturn(List.of(
                new UsuarioSistemaResponse(
                        UUID.randomUUID().toString(),
                        "admin",
                        "Administrador SRI Files",
                        "admin@sri-files.local",
                        "ADMIN",
                        true
                )
        ));

        mockMvc.perform(get("/api/v1/usuarios"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].username").value("admin"));
    }

    @Test
    void obtieneAuditoriaUsuario() throws Exception {
        UUID uuid = UUID.randomUUID();
        when(usuarioSistemaService.obtenerAuditoria(uuid)).thenReturn(List.of(
                new UsuarioAuditoriaResponse(
                        "USUARIO_CREADO",
                        "Usuario creado con rol ADMIN",
                        "admin",
                        "2026-08-15T10:30:00"
                )
        ));

        mockMvc.perform(get("/api/v1/usuarios/{uuid}/auditoria", uuid))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].accion").value("USUARIO_CREADO"));
    }

    @Test
    void creaUsuario() throws Exception {
        when(usuarioSistemaService.crear(any(UsuarioCrearRequest.class), any(UsuarioAutenticadoResponse.class)))
                .thenReturn(new UsuarioSistemaResponse(
                        UUID.randomUUID().toString(),
                        "operador",
                        "Operador Demo",
                        "operador@demo.local",
                        "OPERADOR",
                        true
                ));
        when(authService.obtenerUsuarioDesdeToken("token-demo"))
                .thenReturn(new UsuarioAutenticadoResponse(
                        UUID.randomUUID().toString(),
                        "Administrador SRI Files",
                        "admin@sri-files.local",
                        List.of("ADMIN")
                ));

        mockMvc.perform(post("/api/v1/usuarios")
                        .header("Authorization", "Bearer token-demo")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "username": "operador",
                                  "nombre": "Operador Demo",
                                  "correo": "operador@demo.local",
                                  "password": "demo123",
                                  "rol": "OPERADOR"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.username").value("operador"));
    }

    @Test
    void actualizaEstadoUsuario() throws Exception {
        UUID uuid = UUID.randomUUID();
        when(usuarioSistemaService.actualizarEstado(eq(uuid), any(UsuarioEstadoRequest.class), any(UsuarioAutenticadoResponse.class)))
                .thenReturn(new UsuarioSistemaResponse(
                        uuid.toString(),
                        "operador",
                        "Operador Demo",
                        "operador@demo.local",
                        "OPERADOR",
                        false
                ));
        when(authService.obtenerUsuarioDesdeToken("token-demo"))
                .thenReturn(new UsuarioAutenticadoResponse(
                        UUID.randomUUID().toString(),
                        "Administrador SRI Files",
                        "admin@sri-files.local",
                        List.of("ADMIN")
                ));

        mockMvc.perform(patch("/api/v1/usuarios/{uuid}/estado", uuid)
                        .header("Authorization", "Bearer token-demo")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new UsuarioEstadoRequest(false))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.activo").value(false));
    }

    @Test
    void reseteaPasswordUsuario() throws Exception {
        UUID uuid = UUID.randomUUID();
        when(usuarioSistemaService.resetearPassword(eq(uuid), any(UsuarioPasswordResetRequest.class), any(UsuarioAutenticadoResponse.class)))
                .thenReturn(new UsuarioSistemaResponse(
                        uuid.toString(),
                        "operador",
                        "Operador Demo",
                        "operador@demo.local",
                        "OPERADOR",
                        true
                ));
        when(authService.obtenerUsuarioDesdeToken("token-demo"))
                .thenReturn(new UsuarioAutenticadoResponse(
                        UUID.randomUUID().toString(),
                        "Administrador SRI Files",
                        "admin@sri-files.local",
                        List.of("ADMIN")
                ));

        mockMvc.perform(patch("/api/v1/usuarios/{uuid}/password", uuid)
                        .header("Authorization", "Bearer token-demo")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new UsuarioPasswordResetRequest("nueva123"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("operador"));
    }
}
