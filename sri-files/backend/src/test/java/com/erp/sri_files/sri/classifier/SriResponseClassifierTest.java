package com.erp.sri_files.sri.classifier;

import com.erp.sri_files.sri.model.EstadoComunicacionSri;
import com.erp.sri_files.sri.model.TipoResultadoSri;
import ec.gob.sri.ws.autorizacion.RespuestaComprobante;
import ec.gob.sri.ws.recepcion.RespuestaSolicitud;
import jakarta.xml.ws.WebServiceException;
import org.junit.jupiter.api.Test;

import java.net.ConnectException;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SriResponseClassifierTest {

    private final SriResponseClassifier classifier = new SriResponseClassifier();

    @Test
    void clasificaCodigo43() {
        var res = classifier.clasificar("43", "CLAVE ACCESO REGISTRADA");

        assertEquals(TipoResultadoSri.CLAVE_REGISTRADA, res.getTipo());
        assertEquals("43", res.getCodigo());
        assertFalse(res.isReintentable());
        assertTrue(res.isRequiereConsultaAutorizacion());
    }

    @Test
    void clasificaCodigo43PorCodigoAunqueElMensajeNoLoDiga() {
        var res = classifier.clasificar("43", "comprobante ya registrado previamente");

        assertEquals(TipoResultadoSri.CLAVE_REGISTRADA, res.getTipo());
        assertTrue(res.isRequiereConsultaAutorizacion());
        assertFalse(res.isReintentable());
    }

    @Test
    void clasificaDevuelta() {
        var res = classifier.clasificar("10", "Comprobante devuelto por validaciones");

        assertEquals(TipoResultadoSri.DEVUELTA, res.getTipo());
        assertFalse(res.isReintentable());
        assertFalse(res.isRequiereConsultaAutorizacion());
    }

    @Test
    void clasificaSinRespuestaCuandoNoHayMensaje() {
        var res = classifier.clasificar(null, null);

        assertEquals(TipoResultadoSri.SIN_RESPUESTA, res.getTipo());
    }

    @Test
    void clasificaTimeoutDeSocketComoTransitorio() {
        var res = classifier.clasificarExcepcion(
                new SocketTimeoutException("Read timed out"), "RECEPCION", 1500);

        assertEquals(TipoResultadoSri.ERROR_TRANSITORIO, res.getTipo());
        assertEquals(EstadoComunicacionSri.TIMEOUT, res.getEstadoComunicacion());
        assertTrue(res.isReintentable());
        assertTrue(res.isRequiereConsultaAutorizacion());
    }

    @Test
    void clasificaConnectionResetComoTransitorio() {
        var res = classifier.clasificarExcepcion(
                new SocketException("Connection reset"), "RECEPCION", 1200);

        assertEquals(EstadoComunicacionSri.CONNECTION_RESET, res.getEstadoComunicacion());
        assertTrue(res.isReintentable());
        assertTrue(res.isRequiereConsultaAutorizacion());
    }

    @Test
    void clasificaConnectionRefusedComoTransitorio() {
        var res = classifier.clasificarExcepcion(
                new ConnectException("Connection refused"), "RECEPCION", 900);

        assertEquals(EstadoComunicacionSri.CONNECTION_RESET, res.getEstadoComunicacion());
        assertTrue(res.isReintentable());
    }

    @Test
    void clasificaHostDesconocidoComoSriNoDisponible() {
        var res = classifier.clasificarExcepcion(
                new UnknownHostException("cel.sri.gob.ec"), "RECEPCION", 800);

        assertEquals(EstadoComunicacionSri.SRI_NO_DISPONIBLE, res.getEstadoComunicacion());
        assertTrue(res.isReintentable());
        assertTrue(res.isRequiereConsultaAutorizacion());
    }

    @Test
    void noReintentaErroresFuncionales() {
        var res = classifier.clasificarExcepcion(
                new IllegalStateException("XML mal formado"), "RECEPCION", 500);

        assertEquals(TipoResultadoSri.ERROR_NO_RECUPERABLE, res.getTipo());
        assertFalse(res.isReintentable());
        assertFalse(res.isRequiereConsultaAutorizacion());
    }

    @Test
    void extraeCausaRaizDeUnWebServiceException() {
        var res = classifier.clasificarExcepcion(
                new WebServiceException(new SocketException("Connection reset")), "RECEPCION", 300);

        assertEquals(EstadoComunicacionSri.CONNECTION_RESET, res.getEstadoComunicacion());
        assertTrue(res.isReintentable());
    }

    @Test
    void recepcionRecibida() {
        RespuestaSolicitud rs = new RespuestaSolicitud();
        rs.setEstado("RECIBIDA");

        var res = classifier.clasificarRecepcion(rs, 100);

        assertEquals(TipoResultadoSri.RECIBIDA, res.getTipo());
        assertFalse(res.isRequiereConsultaAutorizacion());
    }

    @Test
    void recepcionDevueltaSinMensajes() {
        var res = classifier.clasificarRecepcion(new RespuestaSolicitud(), 100);

        assertEquals(TipoResultadoSri.DEVUELTA, res.getTipo());
    }

    @Test
    void recepcionNulaEsInciertaYReintentable() {
        var res = classifier.clasificarRecepcion(null, 100);

        assertEquals(TipoResultadoSri.SIN_RESPUESTA, res.getTipo());
        assertTrue(res.isReintentable());
        assertTrue(res.isRequiereConsultaAutorizacion());
    }

    @Test
    void autorizacionNulaEsIncierta() {
        var res = classifier.clasificarAutorizacion(null, 200);

        assertEquals(TipoResultadoSri.SIN_RESPUESTA, res.getTipo());
        assertTrue(res.isReintentable());
        assertTrue(res.isRequiereConsultaAutorizacion());
    }

    @Test
    void autorizacionSinAutorizacionesEsIncierta() {
        RespuestaComprobante rc = new RespuestaComprobante();
        rc.setAutorizaciones(new RespuestaComprobante.Autorizaciones());

        var res = classifier.clasificarAutorizacion(rc, 200);

        assertEquals(TipoResultadoSri.SIN_RESPUESTA, res.getTipo());
        assertTrue(res.isRequiereConsultaAutorizacion());
    }
}