package com.erp.bandred.repositories;

import com.erp.bandred.models.Recaudaxcaja;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface RecaudaxcajaR extends JpaRepository<Recaudaxcaja, Long> {
    @Query(value = "select * from recaudaxcaja where idrecaudaxcaja = ?1", nativeQuery = true)
    Recaudaxcaja findByIdRecaudaxcaja(Long idrecaudaxcaja);
    @Query(value = "select * from recaudaxcaja rc where rc.idusuario_usuarios = ?1 order by rc.idrecaudaxcaja desc limit 1", nativeQuery = true)
    Recaudaxcaja findLastConectionAll(Long idusuario);
}
