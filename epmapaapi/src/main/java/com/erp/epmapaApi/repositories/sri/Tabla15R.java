package com.erp.epmapaApi.repositories.sri;

import com.erp.epmapaApi.models.sri.Tabla15;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface Tabla15R extends JpaRepository<Tabla15, Long> {
    @Query(value = "select nomtabla15 from tabla15 where codtabla15 = ?1", nativeQuery = true)
    String getNombre(String cod);
}
