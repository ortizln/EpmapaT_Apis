package com.erp.sri_files.service;

import com.erp.sri_files.domain.documento.DocumentoElectronico;
import com.erp.sri_files.domain.documento.Empresa;
import com.erp.sri_files.domain.documento.TipoDocumento;
import com.erp.sri_files.services.FacturaXmlGeneratorService;
import com.erp.sri_files.validation.SriRetencionValidationService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

class DocumentoXmlServiceGuiaRemisionTest {

    private final DocumentoXmlService service = new DocumentoXmlService(
            new ObjectMapper(),
            mock(FacturaXmlGeneratorService.class),
            new SriRetencionValidationService()
    );

    @Test
    void generaXmlBaseDeGuiaRemision() {
        DocumentoElectronico documento = documentoBase();

        String xml = service.generar(documento);

        assertTrue(xml.contains("<guiaRemision id=\"comprobante\" version=\"1.1.0\">"));
        assertTrue(xml.contains("<codDoc>06</codDoc>"));
        assertTrue(xml.contains("<dirPartida>Bodega central</dirPartida>"));
        assertTrue(xml.contains("<identificacionDestinatario>0102030405</identificacionDestinatario>"));
        assertTrue(xml.contains("<motivoTraslado>Entrega programada</motivoTraslado>"));
        assertTrue(xml.contains("<codigoInterno>MAT-001</codigoInterno>"));
        assertTrue(xml.contains("<cantidad>3.00</cantidad>"));
    }

    @Test
    void generaXmlConMultiplesDestinatariosYDetalles() {
        DocumentoElectronico documento = documentoBase();
        documento.setJsonOriginal("""
                {
                  "emisor": {
                    "razonSocial": "Empresa Demo",
                    "nombreComercial": "Comercial Demo",
                    "direccionMatriz": "Matriz Centro",
                    "direccionEstablecimiento": "Sucursal Norte"
                  },
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
                      "motivoTraslado": "Entrega sector norte",
                      "codDocSustento": "01",
                      "numDocSustento": "001-001-000000123",
                      "detalles": [
                        {
                          "codigo": "MAT-001",
                          "descripcion": "Tuberia PVC",
                          "cantidad": "3"
                        },
                        {
                          "codigo": "MAT-002",
                          "descripcion": "Codo PVC",
                          "cantidad": "5"
                        }
                      ]
                    },
                    {
                      "identificacion": "1717171717",
                      "razonSocial": "Cliente Dos",
                      "direccion": "Av. Dos",
                      "motivoTraslado": "Entrega sector sur",
                      "codDocSustento": "01",
                      "numDocSustento": "001-001-000000124",
                      "detalles": [
                        {
                          "codigo": "MED-001",
                          "descripcion": "Medidor",
                          "cantidad": "1"
                        }
                      ]
                    }
                  ]
                }
                """);

        String xml = service.generar(documento);

        assertTrue(xml.contains("<razonSocialDestinatario>Cliente Uno</razonSocialDestinatario>"));
        assertTrue(xml.contains("<razonSocialDestinatario>Cliente Dos</razonSocialDestinatario>"));
        assertTrue(xml.contains("<codigoInterno>MAT-002</codigoInterno>"));
        assertTrue(xml.contains("<codigoInterno>MED-001</codigoInterno>"));
    }

    @Test
    void generaCodigoAdicionalCuandoExisteEnDetalle() {
        DocumentoElectronico documento = documentoBase();
        documento.setJsonOriginal("""
                {
                  "emisor": {
                    "razonSocial": "Empresa Demo",
                    "nombreComercial": "Comercial Demo",
                    "direccionMatriz": "Matriz Centro",
                    "direccionEstablecimiento": "Sucursal Norte"
                  },
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
                      "codigo": "MAT-001",
                      "codigoAdicional": "EXT-7788",
                      "descripcion": "Tuberia PVC",
                      "cantidad": "3"
                    }
                  ]
                }
                """);

        String xml = service.generar(documento);

        assertTrue(xml.contains("<codigoAdicional>EXT-7788</codigoAdicional>"));
    }

    private DocumentoElectronico documentoBase() {
        Empresa empresa = new Empresa();
        empresa.setUuid(UUID.randomUUID());
        empresa.setRuc("1790012345001");
        empresa.setRazonSocial("Empresa Demo");
        empresa.setNombreComercial("Comercial Demo");
        empresa.setDireccionMatriz("Matriz Centro");

        DocumentoElectronico documento = new DocumentoElectronico();
        documento.setUuid(UUID.randomUUID());
        documento.setEmpresa(empresa);
        documento.setTipoDocumento(TipoDocumento.GUIA_REMISION);
        documento.setCodigoDocumento("06");
        documento.setAmbiente((short) 1);
        documento.setFechaEmision(LocalDate.of(2026, 8, 14));
        documento.setEstablecimiento("001");
        documento.setPuntoEmision("002");
        documento.setSecuencial("000000123");
        documento.setNumeroDocumento("001-002-000000123");
        documento.setIdentificacionReceptor("0102030405");
        documento.setRazonSocialReceptor("Cliente Demo");
        documento.setSubtotal(new BigDecimal("10.00"));
        documento.setImpuestos(BigDecimal.ZERO);
        documento.setTotal(new BigDecimal("10.00"));
        documento.setJsonOriginal("""
                {
                  "emisor": {
                    "razonSocial": "Empresa Demo",
                    "nombreComercial": "Comercial Demo",
                    "direccionMatriz": "Matriz Centro",
                    "direccionEstablecimiento": "Sucursal Norte"
                  },
                  "documento": {
                    "fechaInicioTransporte": "14/08/2026",
                    "fechaFinTransporte": "14/08/2026",
                    "direccionPartida": "Bodega central",
                    "placa": "ABC1234",
                    "motivoTraslado": "Entrega programada",
                    "codDocSustento": "01",
                    "numDocSustento": "001-001-000000123",
                    "ruta": "Quito - Bodega"
                  },
                  "receptor": {
                    "identificacion": "0102030405",
                    "razonSocial": "Cliente Demo",
                    "direccion": "Av. Principal"
                  },
                  "detalles": [
                    {
                      "codigo": "MAT-001",
                      "descripcion": "Tuberia PVC",
                      "cantidad": "3"
                    }
                  ]
                }
                """);
        return documento;
    }
}
