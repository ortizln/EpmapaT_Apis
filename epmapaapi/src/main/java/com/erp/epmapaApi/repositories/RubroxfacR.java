package com.erp.epmapaApi.repositories;

import com.erp.epmapaApi.models.Rubroxfac;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface RubroxfacR extends JpaRepository<Rubroxfac, Long> {

    @Query("SELECT r FROM Rubroxfac r LEFT JOIN FETCH r.idrubro_rubros WHERE r.idfactura_facturas.idfactura = :idfactura AND (r.estado IS NULL OR r.estado <> 0)")
    List<Rubroxfac> findByIdfacturaWithRubro(Long idfactura);
}
