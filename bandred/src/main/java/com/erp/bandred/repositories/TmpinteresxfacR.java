package com.erp.bandred.repositories;

import com.erp.bandred.models.Tmpinteresxfac;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.math.BigDecimal;

public interface TmpinteresxfacR extends JpaRepository<Tmpinteresxfac, Long > {
    @Query(value = "SELECT COALESCE((SELECT interesapagar FROM tmpinteresxfac WHERE idfactura = ?1), 0)", nativeQuery = true)
    public BigDecimal interesapagar(Long idfactura);
}
