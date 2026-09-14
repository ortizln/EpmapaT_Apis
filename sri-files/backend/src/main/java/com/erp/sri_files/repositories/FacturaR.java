package com.erp.sri_files.repositories;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.repository.JpaRepository;

import com.erp.sri_files.models.Factura;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import org.springframework.data.domain.Pageable;
import java.time.LocalDateTime;
import java.util.List;

public interface FacturaR extends JpaRepository<Factura, Long>{
    Factura findByIdfactura(Long idfactura);
    @Query("SELECT f FROM Factura f WHERE UPPER(TRIM(COALESCE(f.estado, ''))) = UPPER(TRIM(:estado)) ORDER BY f.idfactura ASC")
    List<Factura> _findByEstado(@Param("estado") String estado, Pageable pageable);
    @Query("SELECT f FROM Factura f WHERE UPPER(TRIM(COALESCE(f.estado, ''))) = UPPER(TRIM(:estado)) ORDER BY f.idfactura ASC")
    Page<Factura> findByEstadoNormalizado(@Param("estado") String estado, Pageable pageable);

    @Query("SELECT f FROM Factura f WHERE f.referencia = :referencia ORDER BY f.idfactura ASC")
    List<Factura> findByReferencia(@Param("referencia") String referencia);

    @Query("SELECT f FROM Factura f WHERE f.identificacioncomprador = :identificacion ORDER BY f.idfactura ASC")
    List<Factura> findByIdentificacioncomprador(@Param("identificacion") String identificacion);

    /**
     * Reclama un lote de facturas para procesar de forma segura y concurrente:
     * solo toma filas cuyo estado coincida, excluye las que tienen un reintento
     * programado en el futuro, excluye las que agotaron el máximo de intentos
     * automáticos y evita procesar dos veces la misma fila (FOR UPDATE SKIP LOCKED).
     */
    @Query(value = "SELECT * FROM fec_factura "
            + "WHERE UPPER(TRIM(COALESCE(estado, ''))) = UPPER(TRIM(:estado)) "
            + "AND NOT EXISTS ("
            + "  SELECT 1 FROM sri_intento_comunicacion i "
            + "  WHERE i.documento_id = fec_factura.idfactura "
            + "  AND i.fecha_proximo_intento IS NOT NULL "
            + "  AND i.fecha_proximo_intento > :ahora "
            + ") "
            + "AND NOT EXISTS ("
            + "  SELECT 1 FROM sri_intento_comunicacion i2 "
            + "  WHERE i2.documento_id = fec_factura.idfactura "
            + "  AND i2.reintentable = TRUE "
            + "  AND i2.numero_intento >= :maxIntentos "
            + ") "
            + "ORDER BY idfactura ASC "
            + "FOR UPDATE SKIP LOCKED LIMIT :limite", nativeQuery = true)
    List<Factura> reclamarLoteParaProcesar(@Param("estado") String estado,
                                           @Param("ahora") LocalDateTime ahora,
                                           @Param("maxIntentos") int maxIntentos,
                                           @Param("limite") int limite);

    /**
     * Recupera facturas que quedaron en proceso (estado P) por un worker
     * que ya no está activo (bloqueo abandonado). El umbral es la fecha límite
     * del último intento de comunicación reclamado. Se excluyen los documentos
     * que agotaron el máximo de intentos automáticos.
     */
    @Query(value = "SELECT * FROM fec_factura "
            + "WHERE UPPER(TRIM(COALESCE(estado, ''))) = UPPER(TRIM(:estado)) "
            + "AND ("
            + "  SELECT MAX(i.fecha_inicio) FROM sri_intento_comunicacion i "
            + "  WHERE i.documento_id = fec_factura.idfactura "
            + "  AND i.servicio IN ('CLAIM', 'RECEPCION') "
            + ") < :umbral "
            + "AND NOT EXISTS ("
            + "  SELECT 1 FROM sri_intento_comunicacion i2 "
            + "  WHERE i2.documento_id = fec_factura.idfactura "
            + "  AND i2.reintentable = TRUE "
            + "  AND i2.numero_intento >= :maxIntentos "
            + ") "
            + "ORDER BY idfactura ASC "
            + "FOR UPDATE SKIP LOCKED LIMIT :limite", nativeQuery = true)
    List<Factura> reclamarBloqueosAbandonados(@Param("estado") String estado,
                                              @Param("umbral") LocalDateTime umbral,
                                              @Param("maxIntentos") int maxIntentos,
                                              @Param("limite") int limite);

    /** Conteo de facturas por estado (para supervisión del flujo SRI). */
    @Query(value = "SELECT UPPER(TRIM(COALESCE(estado, ''))) AS estado, COUNT(*) AS total "
            + "FROM fec_factura GROUP BY UPPER(TRIM(COALESCE(estado, '')))", nativeQuery = true)
    List<Object[]> contarPorEstadoDocumento();
}
