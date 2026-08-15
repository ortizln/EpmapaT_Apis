package com.erp.sri_files.domain.documento;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class LegacyFacturaEstadoTest {

    @Test
    void mapeaEstadoLegacyAAutorizado() {
        LegacyFacturaEstado estado = LegacyFacturaEstado.fromCodigo("A");

        assertEquals("Autorizada", estado.getDescripcion());
        assertEquals(DocumentoEstado.AUTORIZADO, estado.getEstadoObjetivo());
    }

    @Test
    void fallaSiElCodigoNoExiste() {
        assertThrows(IllegalArgumentException.class, () -> LegacyFacturaEstado.fromCodigo("Z"));
    }
}
