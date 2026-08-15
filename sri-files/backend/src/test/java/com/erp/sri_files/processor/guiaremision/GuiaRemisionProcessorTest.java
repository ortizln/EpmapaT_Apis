package com.erp.sri_files.processor.guiaremision;

import com.erp.sri_files.domain.documento.DocumentoElectronico;
import com.erp.sri_files.exceptions.DocumentoRecepcionException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class GuiaRemisionProcessorTest {

    private final GuiaRemisionProcessor processor = new GuiaRemisionProcessor(new ObjectMapper());

    @Test
    void validaGuiaRemisionBasica() {
        DocumentoElectronico documento = new DocumentoElectronico();
        documento.setJsonOriginal("""
                {
                  "documento": {
                    "fechaInicioTransporte": "14/08/2026",
                    "fechaFinTransporte": "14/08/2026",
                    "direccionPartida": "Bodega central",
                    "placa": "ABC1234",
                    "motivoTraslado": "Entrega programada",
                    "codDocSustento": "01",
                    "numDocSustento": "001-001-000000123"
                  },
                  "receptor": {
                    "identificacion": "0102030405",
                    "razonSocial": "Cliente Demo",
                    "direccion": "Av. Principal"
                  },
                  "detalles": [
                    {
                      "descripcion": "Tuberia PVC",
                      "cantidad": "3"
                    }
                  ]
                }
                """);

        assertDoesNotThrow(() -> processor.validar(documento));
    }

    @Test
    void fallaSiNoHayDetalles() {
        DocumentoElectronico documento = new DocumentoElectronico();
        documento.setJsonOriginal("""
                {
                  "documento": {
                    "fechaInicioTransporte": "14/08/2026",
                    "fechaFinTransporte": "14/08/2026",
                    "direccionPartida": "Bodega central",
                    "placa": "ABC1234",
                    "motivoTraslado": "Entrega programada",
                    "codDocSustento": "01",
                    "numDocSustento": "001-001-000000123"
                  },
                  "receptor": {
                    "identificacion": "0102030405",
                    "razonSocial": "Cliente Demo",
                    "direccion": "Av. Principal"
                  },
                  "detalles": []
                }
                """);

        assertThrows(DocumentoRecepcionException.class, () -> processor.validar(documento));
    }

    @Test
    void fallaSiLaPlacaNoTieneFormatoValido() {
        DocumentoElectronico documento = new DocumentoElectronico();
        documento.setJsonOriginal("""
                {
                  "documento": {
                    "fechaInicioTransporte": "14/08/2026",
                    "fechaFinTransporte": "14/08/2026",
                    "direccionPartida": "Bodega central",
                    "placa": "12",
                    "motivoTraslado": "Entrega programada",
                    "codDocSustento": "01",
                    "numDocSustento": "001-001-000000123"
                  },
                  "receptor": {
                    "identificacion": "0102030405",
                    "razonSocial": "Cliente Demo",
                    "direccion": "Av. Principal"
                  },
                  "detalles": [
                    {
                      "descripcion": "Tuberia PVC",
                      "cantidad": "3"
                    }
                  ]
                }
                """);

        assertThrows(DocumentoRecepcionException.class, () -> processor.validar(documento));
    }

    @Test
    void validaGuiaRemisionConDestinatariosMultiples() {
        DocumentoElectronico documento = new DocumentoElectronico();
        documento.setJsonOriginal("""
                {
                  "documento": {
                    "fechaInicioTransporte": "14/08/2026",
                    "fechaFinTransporte": "14/08/2026",
                    "direccionPartida": "Bodega central",
                    "placa": "ABC1234"
                  },
                  "destinatarios": [
                    {
                      "identificacion": "0102030405",
                      "razonSocial": "Cliente Uno",
                      "direccion": "Av. Uno",
                      "motivoTraslado": "Entrega 1",
                      "codDocSustento": "01",
                      "numDocSustento": "001-001-000000123",
                      "detalles": [
                        {
                          "descripcion": "Tuberia PVC",
                          "cantidad": "3"
                        }
                      ]
                    },
                    {
                      "identificacion": "1717171717",
                      "razonSocial": "Cliente Dos",
                      "direccion": "Av. Dos",
                      "motivoTraslado": "Entrega 2",
                      "codDocSustento": "01",
                      "numDocSustento": "001-001-000000124",
                      "detalles": [
                        {
                          "descripcion": "Medidor",
                          "cantidad": "1"
                        }
                      ]
                    }
                  ]
                }
                """);

        assertDoesNotThrow(() -> processor.validar(documento));
    }

    @Test
    void fallaSiFechaFinEsMenorQueFechaInicio() {
        DocumentoElectronico documento = new DocumentoElectronico();
        documento.setJsonOriginal("""
                {
                  "documento": {
                    "fechaInicioTransporte": "15/08/2026",
                    "fechaFinTransporte": "14/08/2026",
                    "direccionPartida": "Bodega central",
                    "placa": "ABC1234",
                    "motivoTraslado": "Entrega programada",
                    "codDocSustento": "01",
                    "numDocSustento": "001-001-000000123"
                  },
                  "receptor": {
                    "identificacion": "0102030405",
                    "razonSocial": "Cliente Demo",
                    "direccion": "Av. Principal"
                  },
                  "detalles": [
                    {
                      "descripcion": "Tuberia PVC",
                      "cantidad": "3"
                    }
                  ]
                }
                """);

        assertThrows(DocumentoRecepcionException.class, () -> processor.validar(documento));
    }

    @Test
    void fallaSiCantidadNoEsPositiva() {
        DocumentoElectronico documento = new DocumentoElectronico();
        documento.setJsonOriginal("""
                {
                  "documento": {
                    "fechaInicioTransporte": "14/08/2026",
                    "fechaFinTransporte": "14/08/2026",
                    "direccionPartida": "Bodega central",
                    "placa": "ABC1234",
                    "motivoTraslado": "Entrega programada",
                    "codDocSustento": "01",
                    "numDocSustento": "001-001-000000123"
                  },
                  "receptor": {
                    "identificacion": "0102030405",
                    "razonSocial": "Cliente Demo",
                    "direccion": "Av. Principal"
                  },
                  "detalles": [
                    {
                      "descripcion": "Tuberia PVC",
                      "cantidad": "0"
                    }
                  ]
                }
                """);

        assertThrows(DocumentoRecepcionException.class, () -> processor.validar(documento));
    }
}
