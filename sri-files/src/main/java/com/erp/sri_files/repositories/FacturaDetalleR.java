package com.erp.sri_files.repositories;


import com.erp.sri_files.interfaces.TotalSinImpuestos;
import com.erp.sri_files.models.FacturaDetalle;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface FacturaDetalleR extends JpaRepository<FacturaDetalle, Long> {
    @Query(value = "select sum(cantidad * preciounitario) as totalsinimpuestos, sum (descuento) as descuento from fec_factura_detalles where idfactura = ?1", nativeQuery = true)
    TotalSinImpuestos getTotalSinImpuestos(Long idfactura);


    @EntityGraph(attributePaths = {"impuestos"})
    @Query("select d from FacturaDetalle d where d.factura.idfactura = :idFactura")
    List<FacturaDetalle> findByFacturaIdWithImpuestos(@Param("idFactura") Long idFactura);
}