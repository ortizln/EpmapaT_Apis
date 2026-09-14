package com.erp.sri_files.services;

import com.erp.sri_files.domain.sri.SriIntentoComunicacion;
import com.erp.sri_files.repositories.sri.SriIntentoRepository;
import com.erp.sri_files.sri.metricas.SriMetricasRegistry;
import com.erp.sri_files.sri.model.ResultadoSri;
import com.erp.sri_files.sri.model.ServicioSri;
import com.erp.sri_files.sri.retry.SriRetryPolicy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Persiste cada intento de comunicación con el SRI en sri_intento_comunicacion
 * y calcula cuándo debe reintentarse un documento.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SriIntentoService {

    private static final int MAX_MENSAJE = 4000;
    private static final int MAX_EXCEPCION = 4000;

    private final SriIntentoRepository intentoRepository;
    private final SriRetryPolicy retryPolicy;
    private final SriMetricasRegistry metricasRegistry;

    public String nuevoCorrelationId() {
        return UUID.randomUUID().toString();
    }

    public int siguienteNumeroIntento(Long documentoId) {
        return intentoRepository.findTopByDocumentoIdOrderByNumeroIntentoDesc(documentoId)
                .map(SriIntentoComunicacion::getNumeroIntento)
                .map(n -> n + 1)
                .orElse(1);
    }

    /** Reintentos programados cuyo momento ya llegó (fecha_proximo_intento &lt;= ahora). */
    public List<SriIntentoComunicacion> reintentosVencidos(LocalDateTime ahora) {
        return intentoRepository.findReintentosVencidos(ahora);
    }

    /** Reintentos que alcanzaron el máximo permitido y deben cerrarse. */
    public List<SriIntentoComunicacion> reintentosAgotados(int maxIntentos) {
        return intentoRepository.findReintentosAgotados(maxIntentos);
    }

    /** Desactiva el reintento automático del documento (cierra el ciclo). */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void marcarNoReintentable(Long documentoId) {
        intentoRepository.marcarNoReintentable(documentoId);
    }

    /**
     * Registra un intento (con o sin claim) y calcula fecha_proximo_intento
     * cuando el resultado es reintentable.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public SriIntentoComunicacion registrarIntento(
            Long documentoId,
            ResultadoSri resultado,
            String tipoDocumento,
            String claveAcceso,
            String ambiente,
            String servicio,
            String endpoint,
            String workerId,
            String correlationId,
            LocalDateTime fechaInicio) {

        int numeroIntento = siguienteNumeroIntento(documentoId);
        LocalDateTime ahora = LocalDateTime.now();

        SriIntentoComunicacion intento = new SriIntentoComunicacion();
        intento.setDocumentoId(documentoId);
        intento.setTipoDocumento(tipoDocumento);
        intento.setClaveAcceso(trunc(claveAcceso, 49));
        intento.setAmbiente(ambiente);
        intento.setServicio(servicio == null ? ServicioSri.RECEPCION.name() : servicio);
        intento.setEndpoint(trunc(endpoint, 4000));
        intento.setNumeroIntento(numeroIntento);
        intento.setFechaInicio(fechaInicio == null ? ahora : fechaInicio);
        intento.setFechaFin(ahora);
        intento.setWorkerId(workerId);

        if (resultado != null) {
            intento.setResultado(resultado.getTipo() == null ? null : resultado.getTipo().name());
            intento.setCodigoSri(trunc(resultado.getCodigo(), 20));
            intento.setMensajeSri(trunc(resultado.getMensaje(), MAX_MENSAJE));
            intento.setInformacionAdicional(trunc(resultado.getInformacionAdicional(), MAX_MENSAJE));
            intento.setTipoError(resultado.getEstadoComunicacion() == null ? null : resultado.getEstadoComunicacion().name());
            intento.setExcepcion(trunc(resultado.getExcepcion(), MAX_EXCEPCION));
            intento.setDuracionMs(resultado.getDuracionMs());
            intento.setReintentable(resultado.isReintentable());
            intento.setRequiereConsulta(resultado.isRequiereConsultaAutorizacion());

            if (resultado.isReintentable()) {
                intento.setFechaProximoIntento(retryPolicy.proximoIntento(ahora, numeroIntento));
            }
        }

        intento.setCorrelationId(trunc(correlationId, 80));
        intento.setCreatedAt(ahora);

        SriIntentoComunicacion guardado = intentoRepository.save(intento);
        if (resultado != null) {
            metricasRegistry.acumular(resultado, intento.getServicio());
        }
        log.info("[SRI][INTENTO] doc={} | intento={} | servicio={} | resultado={} | codigo={} | proximo={}",
                documentoId, numeroIntento, intento.getServicio(),
                intento.getResultado(), intento.getCodigoSri(), intento.getFechaProximoIntento());
        return guardado;
    }

    private static String trunc(String s, int max) {
        if (s == null) return null;
        return s.length() <= max ? s : s.substring(0, max);
    }
}