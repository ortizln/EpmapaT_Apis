package com.erp.sri_files.domain.documento;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TipoDocumentoTest {

    @Test
    void exponeCodigoSriEsperado() {
        assertEquals("01", TipoDocumento.FACTURA.getCodigoSri());
        assertEquals("07", TipoDocumento.RETENCION.getCodigoSri());
    }
}
