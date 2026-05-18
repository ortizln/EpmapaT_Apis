package com.erp.sri_files.repositories;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.repository.JpaRepository;

import com.erp.sri_files.models.Factura;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.awt.print.Pageable;
import java.util.List;

public interface FacturaR extends JpaRepository<Factura, Long>{
    Factura findByIdfactura(Long idfactura);
    @Query("SELECT f FROM Factura f WHERE f.estado = :estado ORDER BY f.idfactura ASC")
    List<Factura> _findByEstado(@Param("estado") String estado, Pageable pageable);
    Page<Factura> findByEstado(String estado, PageRequest pageable);

}
