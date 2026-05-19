package com.erp.epmapaApi.repositories.sri;

import com.erp.epmapaApi.models.sri.FecFactura;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface FecFacturaR extends JpaRepository<FecFactura, Long> {
    FecFactura findByIdfactura(Long idfactura);

    @Query("SELECT DISTINCT f FROM FecFactura f LEFT JOIN FETCH f.pagos WHERE f.referencia = :referencia ORDER BY f.idfactura ASC")
    List<FecFactura> findByReferencia(@Param("referencia") String referencia);

    @Query("SELECT DISTINCT f FROM FecFactura f LEFT JOIN FETCH f.pagos WHERE f.identificacioncomprador = :identificacion ORDER BY f.idfactura ASC")
    List<FecFactura> findByIdentificacioncomprador(@Param("identificacion") String identificacion);
}
