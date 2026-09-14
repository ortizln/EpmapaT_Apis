package com.erp.sri_files.sri.retry;

import com.erp.sri_files.sri.model.ResultadoSri;
import com.erp.sri_files.sri.model.ServicioSri;
import com.erp.sri_files.sri.model.TipoResultadoSri;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SriRetryPolicyTest {

    @Test
    void cargaConfiguracionPorDefecto() {
        SriRetryPolicy policy = new SriRetryPolicy(false, 6, "60,300,900,1800,3600");

        assertFalse(policy.isEnabled());
        assertEquals(6, policy.getMaxAttempts());
        assertEquals(List.of(60L, 300L, 900L, 1800L, 3600L), policy.getDelaysSeconds());
    }

    @Test
    void noReintentaSiLaPoliticaEstaDeshabilitada() {
        SriRetryPolicy off = new SriRetryPolicy(false, 6, "60,300,900,1800,3600");
        SriRetryPolicy on = new SriRetryPolicy(true, 6, "60,300,900,1800,3600");
        var resultado = resultadoTransitorio();

        assertFalse(off.debeReintentar(resultado, 1));
        assertTrue(on.debeReintentar(resultado, 1));
    }

    @Test
    void noReintentaResultadosNoReintentables() {
        SriRetryPolicy on = new SriRetryPolicy(true, 6, "60,300,900,1800,3600");
        var devuelta = ResultadoSri.builder()
                .tipo(TipoResultadoSri.DEVUELTA)
                .reintentable(false)
                .servicio(ServicioSri.RECEPCION.name())
                .build();

        assertFalse(on.debeReintentar(devuelta, 1));
    }

    @Test
    void limitaReintentosAlMaximo() {
        SriRetryPolicy on = new SriRetryPolicy(true, 6, "60,300,900,1800,3600");
        var resultado = resultadoTransitorio();

        assertTrue(on.debeReintentar(resultado, 5));
        assertFalse(on.debeReintentar(resultado, 6));
    }

    @Test
    void aplicaRetardosCrecEnientes() {
        SriRetryPolicy policy = new SriRetryPolicy(true, 6, "60,300,900,1800,3600");

        assertEquals(60L, policy.delaySegundosParaIntento(1));
        assertEquals(300L, policy.delaySegundosParaIntento(2));
        assertEquals(900L, policy.delaySegundosParaIntento(3));
        assertEquals(1800L, policy.delaySegundosParaIntento(4));
        assertEquals(3600L, policy.delaySegundosParaIntento(5));
    }

    @Test
    void retardoCreceCuandoSeSuperanLosEscalones() {
        SriRetryPolicy policy = new SriRetryPolicy(true, 6, "60,300,900,1800,3600");

        assertEquals(7200L, policy.delaySegundosParaIntento(6));
        assertEquals(10800L, policy.delaySegundosParaIntento(7));
    }

    @Test
    void calculaProximoIntento() {
        SriRetryPolicy policy = new SriRetryPolicy(true, 6, "60,300,900,1800,3600");
        LocalDateTime ahora = LocalDateTime.of(2026, 9, 11, 10, 0, 0);

        assertEquals(ahora.plusSeconds(300), policy.proximoIntento(ahora, 2));
    }

    private ResultadoSri resultadoTransitorio() {
        return ResultadoSri.builder()
                .tipo(TipoResultadoSri.ERROR_TRANSITORIO)
                .reintentable(true)
                .requiereConsultaAutorizacion(true)
                .servicio(ServicioSri.RECEPCION.name())
                .build();
    }
}