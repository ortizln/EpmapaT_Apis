package com.erp.sri_files.controller;

import com.erp.sri_files.dto.request.SecuencialRequest;
import com.erp.sri_files.dto.response.SecuencialResponse;
import com.erp.sri_files.exceptions.GlobalExceptionHandler;
import com.erp.sri_files.service.SecuencialService;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class SecuencialControllerTest {

    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Mock
    private SecuencialService secuencialService;

    @BeforeEach
    void setUp() {
        SecuencialController controller = new SecuencialController(secuencialService);
        LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .setMessageConverters(new MappingJackson2HttpMessageConverter(objectMapper))
                .setValidator(validator)
                .build();
    }

    @Test
    void listaSecuencialesPorPuntoEmision() throws Exception {
        UUID puntoEmisionUuid = UUID.randomUUID();
        when(secuencialService.listarPorPuntoEmision(puntoEmisionUuid)).thenReturn(List.of(
                new SecuencialResponse(puntoEmisionUuid.toString(), "FACTURA", 348396L, true),
                new SecuencialResponse(puntoEmisionUuid.toString(), "RETENCION", 15L, false)
        ));

        mockMvc.perform(get("/api/v1/puntos-emision/{puntoEmisionUuid}/secuenciales", puntoEmisionUuid))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].tipoDocumento").value("FACTURA"))
                .andExpect(jsonPath("$[0].valorActual").value(348396));
    }

    @Test
    void actualizaSecuencial() throws Exception {
        UUID puntoEmisionUuid = UUID.randomUUID();
        SecuencialRequest request = new SecuencialRequest(348396L, true);

        when(secuencialService.actualizar(eq(puntoEmisionUuid), eq("FACTURA"), any()))
                .thenReturn(new SecuencialResponse(puntoEmisionUuid.toString(), "FACTURA", 348396L, true));

        mockMvc.perform(put("/api/v1/puntos-emision/{puntoEmisionUuid}/secuenciales/{tipoDocumento}", puntoEmisionUuid, "FACTURA")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tipoDocumento").value("FACTURA"))
                .andExpect(jsonPath("$.activo").value(true));
    }
}
