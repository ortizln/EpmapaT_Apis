package com.erp.sri_files.controller;

import com.erp.sri_files.dto.request.DocumentoRecepcionRequest;
import com.erp.sri_files.dto.response.DocumentoAutorizacionConsultaResponse;
import com.erp.sri_files.dto.response.DocumentoContratoResponse;
import com.erp.sri_files.dto.response.DocumentoAutorizacionManualResponse;
import com.erp.sri_files.dto.response.DocumentoAuditoriaEventoResponse;
import com.erp.sri_files.dto.response.DocumentoAuditoriaResumenResponse;
import com.erp.sri_files.dto.response.DocumentoDetalleResponse;
import com.erp.sri_files.dto.response.DocumentoRecepcionResponse;
import com.erp.sri_files.dto.response.DocumentoEstadoResponse;
import com.erp.sri_files.dto.response.DocumentoListadoItemResponse;
import com.erp.sri_files.dto.response.DocumentoListadoResponse;
import com.erp.sri_files.dto.response.DocumentoConteoResponse;
import com.erp.sri_files.dto.response.DocumentoResumenOperativoResponse;
import com.erp.sri_files.exceptions.GlobalExceptionHandler;
import com.erp.sri_files.service.DocumentoApplicationService;
import com.erp.sri_files.service.DocumentoContratoService;
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
import java.util.Map;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class DocumentoControllerTest {

    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Mock
    private DocumentoApplicationService documentoApplicationService;
    @Mock
    private DocumentoContratoService documentoContratoService;

    @BeforeEach
    void setUp() {
        DocumentoController controller = new DocumentoController(documentoApplicationService, documentoContratoService);
        LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .setMessageConverters(new MappingJackson2HttpMessageConverter(objectMapper))
                .setValidator(validator)
                .build();
    }

    @Test
    void recibeDocumentoYRetornaAccepted() throws Exception {
        DocumentoRecepcionRequest request = new DocumentoRecepcionRequest(
                "FACTURA",
                "ERP-001",
                Map.of("ruc", "1790012345001"),
                Map.of("identificacion", "0102030405"),
                Map.of("total", 12.50),
                List.of(Map.of("codigo", "ITEM-1")),
                List.of(),
                Map.of(),
                new DocumentoRecepcionRequest.CorreoRequest(true, List.of("cliente@correo.com"))
        );

        when(documentoApplicationService.recibir(any(), anyString()))
                .thenReturn(new DocumentoRecepcionResponse(
                        "6b80a443-xxxx-xxxx-xxxx-xxxxxxxxxxxx",
                        "FACTURA",
                        "RECIBIDO",
                        "Documento recibido para procesamiento",
                        false
                ));

        mockMvc.perform(post("/api/v1/documentos")
                        .header("Idempotency-Key", "idem-123")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.tipoDocumento").value("FACTURA"))
                .andExpect(jsonPath("$.estado").value("RECIBIDO"));
    }

    @Test
    void retornaBadRequestSiFaltaTipoDocumento() throws Exception {
        String body = """
                {
                  "externalId": "ERP-001"
                }
                """;

        mockMvc.perform(post("/api/v1/documentos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("DOC_VALIDATION_ERROR"));
    }

    @Test
    void retornaBadRequestSiFaltaExternalId() throws Exception {
        String body = """
                {
                  "tipoDocumento": "FACTURA"
                }
                """;

        mockMvc.perform(post("/api/v1/documentos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("DOC_VALIDATION_ERROR"));
    }

    @Test
    void obtieneEstadoDocumento() throws Exception {
        UUID uuid = UUID.randomUUID();
        when(documentoApplicationService.obtenerEstado(uuid))
                .thenReturn(new DocumentoEstadoResponse(uuid.toString(), "VALIDADO", false));

        mockMvc.perform(get("/api/v1/documentos/{uuid}/estado", uuid))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("VALIDADO"));
    }

    @Test
    void consultaAutorizacionPorClave() throws Exception {
        when(documentoApplicationService.consultarAutorizacionPorClave(
                "0123456789012345678901234567890123456789012345678",
                true
        )).thenReturn(new DocumentoAutorizacionConsultaResponse(
                "0123456789012345678901234567890123456789012345678",
                "AUTORIZADO",
                true,
                "1234567890",
                "2026-08-15T10:20:30",
                "AUTORIZADO",
                true,
                "<autorizacion/>"
        ));

        mockMvc.perform(get("/api/v1/documentos/autorizacion")
                        .param("claveAcceso", "0123456789012345678901234567890123456789012345678")
                        .param("incluirXml", "true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.autorizado").value(true))
                .andExpect(jsonPath("$.estado").value("AUTORIZADO"))
                .andExpect(jsonPath("$.xmlAutorizado").value("<autorizacion/>"));
    }

    @Test
    void consultaAutorizacionManual() throws Exception {
        UUID uuid = UUID.randomUUID();
        when(documentoApplicationService.consultarAutorizacion(uuid))
                .thenReturn(new DocumentoAutorizacionManualResponse(
                        uuid.toString(),
                        "0123456789012345678901234567890123456789012345678",
                        "AUTORIZADO",
                        true,
                        "1234567890",
                        "2026-08-15T10:20:30",
                        "AUTORIZADO",
                        true
                ));

        mockMvc.perform(post("/api/v1/documentos/{uuid}/consultar-autorizacion", uuid))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.autorizado").value(true))
                .andExpect(jsonPath("$.estado").value("AUTORIZADO"));
    }

    @Test
    void obtieneContratoDocumento() throws Exception {
        when(documentoContratoService.obtener("GUIA_REMISION"))
                .thenReturn(new DocumentoContratoResponse("GUIA_REMISION", "/api/v1/documentos", "POST", List.of()));

        mockMvc.perform(get("/api/v1/documentos/contratos/GUIA_REMISION"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tipoDocumento").value("GUIA_REMISION"))
                .andExpect(jsonPath("$.endpoint").value("/api/v1/documentos"));
    }

    @Test
    void listaDocumentosConFiltros() throws Exception {
        when(documentoApplicationService.listar(null, "FACTURA", "AUTORIZADO", "epmapa", 0, 10))
                .thenReturn(new DocumentoListadoResponse(
                        List.of(new DocumentoListadoItemResponse(
                                UUID.randomUUID().toString(),
                                "FACTURA",
                                "001-001-000000321",
                                "Empresa Publica Demo",
                                "2026-08-14",
                                "AUTORIZADO"
                        )),
                        0,
                        10,
                        1,
                        1
                ));

        mockMvc.perform(get("/api/v1/documentos")
                        .param("tipoDocumento", "FACTURA")
                        .param("estado", "AUTORIZADO")
                        .param("busqueda", "epmapa")
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].tipoDocumento").value("FACTURA"))
                .andExpect(jsonPath("$.totalItems").value(1));
    }

    @Test
    void obtieneResumenOperativo() throws Exception {
        when(documentoApplicationService.obtenerResumenOperativo())
                .thenReturn(new DocumentoResumenOperativoResponse(
                        12,
                        3,
                        5,
                        4,
                        2,
                        1,
                        List.of(
                                new DocumentoConteoResponse("FACTURA", 8),
                                new DocumentoConteoResponse("NOTA_CREDITO", 4)
                        ),
                        List.of(
                                new DocumentoConteoResponse("FINALIZADO", 5),
                                new DocumentoConteoResponse("REQUIERE_INTERVENCION", 1)
                        )
                ));

        mockMvc.perform(get("/api/v1/documentos/resumen"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalDocumentos").value(12))
                .andExpect(jsonPath("$.recibidosHoy").value(3))
                .andExpect(jsonPath("$.porTipo[0].clave").value("FACTURA"));
    }

    @Test
    void obtieneAuditoriaReciente() throws Exception {
        when(documentoApplicationService.obtenerAuditoriaReciente())
                .thenReturn(new DocumentoAuditoriaResumenResponse(
                        1,
                        List.of(new DocumentoAuditoriaEventoResponse(
                                10L,
                                UUID.randomUUID().toString(),
                                "FACTURA",
                                "001-001-000000321",
                                "ERP-001",
                                "RECIBIDO",
                                "XML_GENERADO",
                                "XML del documento generado",
                                "SYSTEM",
                                "2026-08-15T18:20:00"
                        ))
                ));

        mockMvc.perform(get("/api/v1/documentos/auditoria"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalEventos").value(1))
                .andExpect(jsonPath("$.eventos[0].tipoDocumento").value("FACTURA"))
                .andExpect(jsonPath("$.eventos[0].estadoNuevo").value("XML_GENERADO"));
    }
}
