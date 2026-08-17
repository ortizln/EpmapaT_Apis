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

class DocumentoXmlServiceNotasTest {

    private final DocumentoXmlService service = new DocumentoXmlService(
            new ObjectMapper(),
            mock(FacturaXmlGeneratorService.class),
            new SriRetencionValidationService()
    );

    @Test
    void generaNotaCreditoConMultiplesDetalles() {
        DocumentoElectronico documento = documentoBase(TipoDocumento.NOTA_CREDITO, "04");
        documento.setJsonOriginal("""
                {
                  "emisor": {
                    "razonSocial": "Empresa Demo",
                    "nombreComercial": "Comercial Demo",
                    "direccionMatriz": "Matriz Centro",
                    "direccionEstablecimiento": "Sucursal Norte"
                  },
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
                      "codigoAdicional": "EXT-001",
                      "descripcion": "Tuberia PVC",
                      "cantidad": "2",
                      "precioUnitario": "10.00",
                      "descuento": "0.00",
                      "precioTotalSinImpuesto": "20.00",
                      "baseImponible": "20.00",
                      "valorImpuesto": "0.00"
                    },
                    {
                      "codigo": "MAT-002",
                      "descripcion": "Codo PVC",
                      "cantidad": "1",
                      "precioUnitario": "5.00",
                      "descuento": "0.00",
                      "precioTotalSinImpuesto": "5.00",
                      "baseImponible": "5.00",
                      "valorImpuesto": "0.00"
                    }
                  ]
                }
                """);

        String xml = service.generar(documento);

        assertTrue(xml.contains("<notaCredito id=\"comprobante\" version=\"1.0.0\">"));
        assertTrue(xml.contains("<codigoInterno>MAT-001</codigoInterno>"));
        assertTrue(xml.contains("<codigoAdicional>EXT-001</codigoAdicional>"));
        assertTrue(xml.contains("<codigoInterno>MAT-002</codigoInterno>"));
    }

    @Test
    void generaLiquidacionCompraConDetalles() {
        DocumentoElectronico documento = documentoBase(TipoDocumento.LIQUIDACION_COMPRA, "03");
        documento.setJsonOriginal("""
                {
                  "emisor": {
                    "razonSocial": "Empresa Demo",
                    "nombreComercial": "Comercial Demo",
                    "direccionMatriz": "Matriz Centro",
                    "direccionEstablecimiento": "Sucursal Norte"
                  },
                  "receptor": {
                    "tipoIdentificacion": "04",
                    "identificacion": "1717171717",
                    "razonSocial": "Proveedor Demo",
                    "direccion": "Av. Proveedor"
                  },
                  "detalles": [
                    {
                      "codigo": "MAT-001",
                      "codigoAdicional": "EXT-001",
                      "descripcion": "Compra de materiales",
                      "cantidad": "2",
                      "precioUnitario": "50.00",
                      "descuento": "0.00",
                      "precioTotalSinImpuesto": "100.00"
                    }
                  ]
                }
                """);
        documento.setRazonSocialReceptor("Proveedor Demo");
        documento.setIdentificacionReceptor("1717171717");
        documento.setSubtotal(new BigDecimal("100.00"));
        documento.setImpuestos(new BigDecimal("12.00"));
        documento.setTotal(new BigDecimal("112.00"));

        String xml = service.generar(documento);

        assertTrue(xml.contains("<liquidacionCompra id=\"comprobante\" version=\"1.0.0\">"));
        assertTrue(xml.contains("<codDoc>03</codDoc>"));
        assertTrue(xml.contains("<razonSocialProveedor>Proveedor Demo</razonSocialProveedor>"));
        assertTrue(xml.contains("<codigoPrincipal>MAT-001</codigoPrincipal>"));
    }

    @Test
    void generaNotaDebitoConMultiplesMotivos() {
        DocumentoElectronico documento = documentoBase(TipoDocumento.NOTA_DEBITO, "05");
        documento.setJsonOriginal("""
                {
                  "emisor": {
                    "razonSocial": "Empresa Demo",
                    "nombreComercial": "Comercial Demo",
                    "direccionMatriz": "Matriz Centro",
                    "direccionEstablecimiento": "Sucursal Norte"
                  },
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
                    },
                    {
                      "razon": "Servicio adicional",
                      "valor": "2.50"
                    }
                  ]
                }
                """);

        String xml = service.generar(documento);

        assertTrue(xml.contains("<notaDebito id=\"comprobante\" version=\"1.0.0\">"));
        assertTrue(xml.contains("<razon>Recargo por diferencia</razon>"));
        assertTrue(xml.contains("<razon>Servicio adicional</razon>"));
    }

    private DocumentoElectronico documentoBase(TipoDocumento tipoDocumento, String codigoDocumento) {
        Empresa empresa = new Empresa();
        empresa.setUuid(UUID.randomUUID());
        empresa.setRuc("1790012345001");
        empresa.setRazonSocial("Empresa Demo");
        empresa.setNombreComercial("Comercial Demo");
        empresa.setDireccionMatriz("Matriz Centro");

        DocumentoElectronico documento = new DocumentoElectronico();
        documento.setUuid(UUID.randomUUID());
        documento.setEmpresa(empresa);
        documento.setTipoDocumento(tipoDocumento);
        documento.setCodigoDocumento(codigoDocumento);
        documento.setAmbiente((short) 1);
        documento.setFechaEmision(LocalDate.of(2026, 8, 14));
        documento.setEstablecimiento("001");
        documento.setPuntoEmision("002");
        documento.setSecuencial("000000123");
        documento.setNumeroDocumento("001-002-000000123");
        documento.setIdentificacionReceptor("0102030405");
        documento.setRazonSocialReceptor("Cliente Demo");
        documento.setSubtotal(new BigDecimal("25.00"));
        documento.setImpuestos(BigDecimal.ZERO);
        documento.setTotal(new BigDecimal("25.00"));
        return documento;
    }
}
