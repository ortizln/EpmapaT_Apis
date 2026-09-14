package com.erp.sri_files.repositories.sri;

import com.erp.sri_files.domain.sri.SriIntentoComunicacion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface SriIntentoRepository extends JpaRepository<SriIntentoComunicacion, Long> {

    Optional<SriIntentoComunicacion> findTopByDocumentoIdOrderByNumeroIntentoDesc(Long documentoId);

    List<SriIntentoComunicacion> findTop20ByDocumentoIdAndRequiereConsultaTrueOrderByFechaFinDesc(Long documentoId);

    @Query("SELECT i FROM SriIntentoComunicacion i WHERE i.claveAcceso = :claveAcceso ORDER BY i.numeroIntento DESC")
    List<SriIntentoComunicacion> findByClaveAcceso(@Param("claveAcceso") String claveAcceso);

    @Query("SELECT COUNT(i) FROM SriIntentoComunicacion i "
            + "WHERE i.documentoId = :documentoId AND i.createdAt >= :desde")
    long contarIntentoDesde(@Param("documentoId") Long documentoId, @Param("desde") LocalDateTime desde);

    @Modifying
    @Query("UPDATE SriIntentoComunicacion i SET i.requiereConsulta = false, i.resultado = 'RECONCILIADA' "
            + "WHERE i.documentoId = :documentoId AND i.requiereConsulta = true")
    int marcarConciliadas(@Param("documentoId") Long documentoId);

    /**
     * Último intento reintentable de cada documento cuya fecha_proximo_intento
     * ya venció (le toca reintentar ahora).
     */
    @Query("SELECT i FROM SriIntentoComunicacion i "
            + "WHERE i.reintentable = true "
            + "AND i.fechaProximoIntento IS NOT NULL "
            + "AND i.fechaProximoIntento <= :ahora "
            + "AND i.numeroIntento = (SELECT MAX(ii.numeroIntento) FROM SriIntentoComunicacion ii "
            + "                         WHERE ii.documentoId = i.documentoId) "
            + "ORDER BY i.fechaProximoIntento ASC")
    List<SriIntentoComunicacion> findReintentosVencidos(@Param("ahora") LocalDateTime ahora);

    /**
     * Último intento reintentable de cada documento que alcanzó o superó
     * el máximo de intentos permitidos (debe cerrarse para no reintentar en ciclo infinito).
     */
    @Query("SELECT i FROM SriIntentoComunicacion i "
            + "WHERE i.reintentable = true "
            + "AND i.numeroIntento = (SELECT MAX(ii.numeroIntento) FROM SriIntentoComunicacion ii "
            + "                         WHERE ii.documentoId = i.documentoId) "
            + "AND i.numeroIntento >= :maxIntentos "
            + "ORDER BY i.documentoId ASC")
    List<SriIntentoComunicacion> findReintentosAgotados(@Param("maxIntentos") int maxIntentos);

    @Modifying
    @Query("UPDATE SriIntentoComunicacion i SET i.reintentable = false, i.fechaProximoIntento = null "
            + "WHERE i.documentoId = :documentoId")
    int marcarNoReintentable(@Param("documentoId") Long documentoId);

    // ==== Agregados para supervisión (Fase 6) ====

    @Query("SELECT i.resultado, COUNT(i) FROM SriIntentoComunicacion i GROUP BY i.resultado")
    List<Object[]> contarPorResultado();

    @Query("SELECT i.servicio, COUNT(i) FROM SriIntentoComunicacion i GROUP BY i.servicio")
    List<Object[]> contarPorServicio();

    @Query("SELECT i.tipoError, COUNT(i) FROM SriIntentoComunicacion i GROUP BY i.tipoError")
    List<Object[]> contarPorTipoError();

    @Query("SELECT AVG(i.duracionMs) FROM SriIntentoComunicacion i")
    Double promedioDuracionMs();

    @Query("SELECT COUNT(i) FROM SriIntentoComunicacion i "
            + "WHERE i.reintentable = true AND i.fechaProximoIntento IS NOT NULL "
            + "AND i.fechaProximoIntento <= :ahora")
    long contarPendientesReintento(@Param("ahora") LocalDateTime ahora);

    List<SriIntentoComunicacion> findTop50ByOrderByFechaFinDesc();
}