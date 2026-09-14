package com.erp.sri_files.sri.circuitbreaker;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SriCircuitBreakerTest {

    private static final String RECEPCION = "RECEPCION";

    @Test
    void deshabilitadoSiemprePermite() {
        SriCircuitBreaker cb = new SriCircuitBreaker(false, 3, 1000, 1);
        assertEquals(SriCircuitBreaker.Estado.CERRADO, cb.getEstado(RECEPCION));
        assertTrue(cb.puedeProceder(RECEPCION));
        cb.onFallo(RECEPCION);
        assertTrue(cb.puedeProceder(RECEPCION));
    }

    @Test
    void fallosPorDebajoDelUmbralMantienenCerrado() {
        SriCircuitBreaker cb = new SriCircuitBreaker(true, 3, 1000, 1);
        cb.onFallo(RECEPCION);
        cb.onFallo(RECEPCION);
        assertEquals(SriCircuitBreaker.Estado.CERRADO, cb.getEstado(RECEPCION));
        assertTrue(cb.puedeProceder(RECEPCION));
    }

    @Test
    void alcanzarElUmbralAbreElCircuito() {
        SriCircuitBreaker cb = new SriCircuitBreaker(true, 3, 1000, 1);
        cb.onFallo(RECEPCION);
        cb.onFallo(RECEPCION);
        cb.onFallo(RECEPCION);
        assertEquals(SriCircuitBreaker.Estado.ABIERTO, cb.getEstado(RECEPCION));
        assertFalse(cb.puedeProceder(RECEPCION));
    }

    @Test
    void exitoReseteaLosFallos() {
        SriCircuitBreaker cb = new SriCircuitBreaker(true, 3, 1000, 1);
        cb.onFallo(RECEPCION);
        cb.onFallo(RECEPCION);
        cb.onExito(RECEPCION);
        cb.onFallo(RECEPCION);
        cb.onFallo(RECEPCION);
        assertEquals(SriCircuitBreaker.Estado.CERRADO, cb.getEstado(RECEPCION));
    }

    @Test
    void trasOpenMillisPasaASemiAbiertoConUnSondeo() {
        SriCircuitBreaker cb = new SriCircuitBreaker(true, 1, 1000, 1);
        cb.onFallo(RECEPCION);
        assertEquals(SriCircuitBreaker.Estado.ABIERTO, cb.getEstado(RECEPCION));
        assertFalse(cb.puedeProceder(RECEPCION));
        sleep(1150);
        // Primer llamado tras la espera: semisondeo permitido
        assertTrue(cb.puedeProceder(RECEPCION));
        // El segundo ya no pasa (solo 1 sondeo mientras el resultado no llegue)
        assertFalse(cb.puedeProceder(RECEPCION));
    }

    @Test
    void sondeoExitosoCierraElCircuito() {
        SriCircuitBreaker cb = new SriCircuitBreaker(true, 1, 1000, 1);
        cb.onFallo(RECEPCION);
        sleep(1150);
        assertTrue(cb.puedeProceder(RECEPCION));
        cb.onExito(RECEPCION);
        assertEquals(SriCircuitBreaker.Estado.CERRADO, cb.getEstado(RECEPCION));
        assertTrue(cb.puedeProceder(RECEPCION));
    }

    @Test
    void sondeoFallidoVuelveAAbrir() {
        SriCircuitBreaker cb = new SriCircuitBreaker(true, 1, 1000, 1);
        cb.onFallo(RECEPCION);
        sleep(1150);
        assertTrue(cb.puedeProceder(RECEPCION));
        cb.onFallo(RECEPCION);
        assertEquals(SriCircuitBreaker.Estado.ABIERTO, cb.getEstado(RECEPCION));
    }

    private static void sleep(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}