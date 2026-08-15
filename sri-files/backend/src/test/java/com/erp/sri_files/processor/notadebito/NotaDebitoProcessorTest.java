package com.erp.sri_files.processor.notadebito;

import com.erp.sri_files.domain.documento.DocumentoElectronico;
import com.erp.sri_files.exceptions.DocumentoRecepcionException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class NotaDebitoProcessorTest {

    private final NotaDebitoProcessor processor = new NotaDebitoProcessor(new ObjectMapper());

    @Test
    void validaNotaDebitoConMotivos() {
        DocumentoElectronico documento = new DocumentoElectronico();
        documento.setJsonOriginal("""
                {
                  "documento": {
                    "motivo": "Recargo operativo",
                    "numeroDocumentoModificado": "001-001-000000123",
                    "fechaEmisionDocumentoModificado": "14/08/2026"
                  },
                  "receptor": {
                    "identificacion": "0102030405",
                    "razonSocial": "Cliente Demo"
                  },
                  "motivos": [
                    {
                      "razon": "Recargo por diferencia",
                      "valor": "5.00"
                    }
                  ]
                }
                """);

        assertDoesNotThrow(() -> processor.validar(documento));
    }

    @Test
    void fallaSiMotivoTieneValorNoPositivo() {
        DocumentoElectronico documento = new DocumentoElectronico();
        documento.setJsonOriginal("""
                {
                  "documento": {
                    "motivo": "Recargo operativo",
                    "numeroDocumentoModificado": "001-001-000000123",
                    "fechaEmisionDocumentoModificado": "14/08/2026"
                  },
                  "receptor": {
                    "identificacion": "0102030405",
                    "razonSocial": "Cliente Demo"
                  },
                  "motivos": [
                    {
                      "razon": "Recargo por diferencia",
                      "valor": "0"
                    }
                  ]
                }
                """);

        assertThrows(DocumentoRecepcionException.class, () -> processor.validar(documento));
    }
}
