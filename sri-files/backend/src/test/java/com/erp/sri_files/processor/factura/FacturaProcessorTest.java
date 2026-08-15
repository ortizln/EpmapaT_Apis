package com.erp.sri_files.processor.factura;

import com.erp.sri_files.domain.documento.DocumentoElectronico;
import com.erp.sri_files.exceptions.DocumentoRecepcionException;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class FacturaProcessorTest {

    private final FacturaProcessor processor = new FacturaProcessor();

    @Test
    void validaFacturaBasica() {
        DocumentoElectronico documento = new DocumentoElectronico();
        documento.setFechaEmision(LocalDate.of(2026, 8, 14));
        documento.setIdentificacionReceptor("0102030405");
        documento.setTotal(new BigDecimal("11.20"));

        assertDoesNotThrow(() -> processor.validar(documento));
    }

    @Test
    void fallaSiFaltaIdentificacion() {
        DocumentoElectronico documento = new DocumentoElectronico();
        documento.setFechaEmision(LocalDate.of(2026, 8, 14));
        documento.setTotal(new BigDecimal("11.20"));

        assertThrows(DocumentoRecepcionException.class, () -> processor.validar(documento));
    }
}
