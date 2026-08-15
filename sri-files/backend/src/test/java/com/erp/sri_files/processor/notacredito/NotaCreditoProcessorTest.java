package com.erp.sri_files.processor.notacredito;

import com.erp.sri_files.domain.documento.DocumentoElectronico;
import com.erp.sri_files.exceptions.DocumentoRecepcionException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class NotaCreditoProcessorTest {

    private final NotaCreditoProcessor processor = new NotaCreditoProcessor(new ObjectMapper());

    @Test
    void validaNotaCreditoConDetalles() {
        DocumentoElectronico documento = new DocumentoElectronico();
        documento.setJsonOriginal("""
                {
                  "documento": {
                    "motivo": "Devolucion parcial",
                    "numeroDocumentoModificado": "001-001-000000123",
                    "fechaEmisionDocumentoModificado": "14/08/2026"
                  },
                  "receptor": {
                    "identificacion": "0102030405",
                    "razonSocial": "Cliente Demo"
                  },
                  "detalles": [
                    {
                      "codigo": "MAT-001",
                      "descripcion": "Tuberia PVC",
                      "cantidad": "2",
                      "precioUnitario": "10.00"
                    }
                  ]
                }
                """);

        assertDoesNotThrow(() -> processor.validar(documento));
    }

    @Test
    void fallaSiNumeroDocumentoModificadoEsInvalido() {
        DocumentoElectronico documento = new DocumentoElectronico();
        documento.setJsonOriginal("""
                {
                  "documento": {
                    "motivo": "Devolucion parcial",
                    "numeroDocumentoModificado": "123",
                    "fechaEmisionDocumentoModificado": "14/08/2026"
                  },
                  "receptor": {
                    "identificacion": "0102030405",
                    "razonSocial": "Cliente Demo"
                  },
                  "detalles": [
                    {
                      "descripcion": "Tuberia PVC",
                      "cantidad": "2",
                      "precioUnitario": "10.00"
                    }
                  ]
                }
                """);

        assertThrows(DocumentoRecepcionException.class, () -> processor.validar(documento));
    }
}
