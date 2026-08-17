package com.erp.sri_files.service;

import com.erp.sri_files.dto.request.DocumentoRecepcionRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DocumentoContratoFixturesTest {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static Validator validator;

    @BeforeAll
    static void setUpValidator() {
        validator = Validation.buildDefaultValidatorFactory().getValidator();
    }

    @Test
    void fixturesDeContratosSonValidos() throws Exception {
        List<String> fixtures = List.of(
                "factura.json",
                "liquidacion-compra.json",
                "nota-credito.json",
                "nota-debito.json",
                "guia-remision.json",
                "retencion.json"
        );

        for (String fixture : fixtures) {
            DocumentoRecepcionRequest request = readFixture(fixture);
            assertNotNull(request, "No se pudo cargar el fixture " + fixture);
            assertTrue(validator.validate(request).isEmpty(), "El fixture " + fixture + " deberia pasar validaciones");
        }
    }

    @Test
    void fixturesCubrenLosSeisTiposDocumentales() throws Exception {
        assertEquals("FACTURA", readFixture("factura.json").tipoDocumento());
        assertEquals("LIQUIDACION_COMPRA", readFixture("liquidacion-compra.json").tipoDocumento());
        assertEquals("NOTA_CREDITO", readFixture("nota-credito.json").tipoDocumento());
        assertEquals("NOTA_DEBITO", readFixture("nota-debito.json").tipoDocumento());
        assertEquals("GUIA_REMISION", readFixture("guia-remision.json").tipoDocumento());
        assertEquals("RETENCION", readFixture("retencion.json").tipoDocumento());
    }

    private DocumentoRecepcionRequest readFixture(String filename) throws Exception {
        try (InputStream inputStream = getClass().getResourceAsStream("/fixtures/documentos/" + filename)) {
            return OBJECT_MAPPER.readValue(inputStream, DocumentoRecepcionRequest.class);
        }
    }
}
