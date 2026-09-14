package com.erp.sri_files.sri.retry;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SriReintentoDeciderTest {

    private static final LocalDateTime AHORA = LocalDateTime.of(2026, 9, 12, 10, 0, 0);
    private static final int MAX = 6;

    @Test
    void noReintentableDeviaIgnoar() {
        assertEquals(SriReintentoDecision.IGNORAR,
                SriReintentoDecider.decidir(false, 2, MAX, AHORA.minusMinutes(5), AHORA));
    }

    @Test
    void ahoraEsNullDevuelveAunNo() {
        assertEquals(SriReintentoDecision.AUN_NO,
                SriReintentoDecider.decidir(true, 2, MAX, null, AHORA));
    }

    @Test
    void proximoEnFuturoDevuelveAunNo() {
        assertEquals(SriReintentoDecision.AUN_NO,
                SriReintentoDecider.decidir(true, 3, MAX, AHORA.plusMinutes(5), AHORA));
    }

    @Test
    void justoEnLaFechaYaEsConsultar() {
        assertEquals(SriReintentoDecision.CONSULTAR,
                SriReintentoDecider.decidir(true, 3, MAX, AHORA, AHORA));
    }

    @Test
    void vencidoEsConsultar() {
        assertEquals(SriReintentoDecision.CONSULTAR,
                SriReintentoDecider.decidir(true, 3, MAX, AHORA.minusHours(1), AHORA));
    }

    @Test
    void alcanzoElMaximoEsAgotado() {
        assertEquals(SriReintentoDecision.AGOTADO,
                SriReintentoDecider.decidir(true, MAX, MAX, AHORA, AHORA));
    }

    @Test
    void superoElMaximoEsAgotado() {
        assertEquals(SriReintentoDecision.AGOTADO,
                SriReintentoDecider.decidir(true, 9, MAX, AHORA.minusHours(1), AHORA));
    }
}