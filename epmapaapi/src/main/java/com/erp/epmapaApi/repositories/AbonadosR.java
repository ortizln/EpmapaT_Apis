package com.erp.epmapaApi.repositories;

import com.erp.epmapaApi.models.Abonados;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface AbonadosR extends JpaRepository<Abonados, Long> {
    @Query("SELECT a FROM Abonados a WHERE a.idcliente_clientes.idcliente = :idcliente OR a.idresponsable.idcliente = :idcliente")
    List<Abonados> findCuentasByCliente(@Param("idcliente") Long idcliente);

    @Query("SELECT a FROM Abonados a WHERE a.idresponsable.idcliente = :idcliente")
    List<Abonados> findByResponsable(@Param("idcliente") Long idcliente);

    @Query("SELECT a FROM Abonados a WHERE a.idresponsable.cedula = :identificacion")
    List<Abonados> findByIdentificacionResponsable(@Param("identificacion") String identificacion);
}
