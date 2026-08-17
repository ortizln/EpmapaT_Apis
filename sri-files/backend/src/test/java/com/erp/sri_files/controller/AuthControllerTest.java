package com.erp.sri_files.controller;

import com.erp.sri_files.dto.request.LoginRequest;
import com.erp.sri_files.dto.response.LoginResponse;
import com.erp.sri_files.dto.response.UsuarioAutenticadoResponse;
import com.erp.sri_files.exceptions.AuthException;
import com.erp.sri_files.exceptions.GlobalExceptionHandler;
import com.erp.sri_files.service.AuthService;
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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class AuthControllerTest {

    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Mock
    private AuthService authService;

    @BeforeEach
    void setUp() {
        AuthController controller = new AuthController(authService);
        LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .setMessageConverters(new MappingJackson2HttpMessageConverter(objectMapper))
                .setValidator(validator)
                .build();
    }

    @Test
    void loginRetornaUsuarioYTokens() throws Exception {
        when(authService.login(any())).thenReturn(new LoginResponse(
                "token-demo",
                "refresh-demo",
                new UsuarioAutenticadoResponse("1", "Administrador SRI Files", "admin@sri-files.local", List.of("ADMIN"), List.of("ROL_VER", "USUARIO_VER"))
        ));

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new LoginRequest("admin", "admin123"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("token-demo"))
                .andExpect(jsonPath("$.usuario.nombre").value("Administrador SRI Files"));
    }

    @Test
    void loginValidaCamposRequeridos() throws Exception {
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"\",\"password\":\"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("DOC_VALIDATION_ERROR"));
    }

    @Test
    void meRetornaUsuarioAutenticado() throws Exception {
        when(authService.obtenerUsuarioDesdeToken("token-demo"))
                .thenReturn(new UsuarioAutenticadoResponse("1", "Administrador SRI Files", "admin@sri-files.local", List.of("ADMIN"), List.of("ROL_VER", "USUARIO_VER")));

        mockMvc.perform(get("/api/v1/auth/me")
                        .header("Authorization", "Bearer token-demo"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.correo").value("admin@sri-files.local"))
                .andExpect(jsonPath("$.permisos[0]").value("ROL_VER"));
    }

    @Test
    void meRetornaUnauthorizedConTokenInvalido() throws Exception {
        doThrow(new AuthException("Token invalido"))
                .when(authService).obtenerUsuarioDesdeToken(eq("token-invalido"));

        mockMvc.perform(get("/api/v1/auth/me")
                        .header("Authorization", "Bearer token-invalido"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("AUTH_ERROR"));
    }
}
