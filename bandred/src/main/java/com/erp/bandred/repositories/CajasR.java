package com.erp.bandred.repositories;

import com.erp.bandred.interfaces.LastConection_int;
import com.erp.bandred.interfaces.NroFactura_int;
import com.erp.bandred.models.Cajas;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface CajasR extends JpaRepository<Cajas , Long> {
    @Query(value = "select p.establecimiento, c.codigo, rc.facfin + 1 as secuencial, rc.estado from cajas c join usuarios u on c.idusuario_usuarios = u.idusuario join ptoemision p on c.idptoemision_ptoemision = p.idptoemision join recaudaxcaja rc on rc.idcaja_cajas = c.idcaja where u.idusuario = ?1 order by rc.idrecaudaxcaja desc limit 1", nativeQuery = true)
    NroFactura_int buildNroFactura(Long idusuario);
    @Query(value = "select rc.idrecaudaxcaja, rc.fechainiciolabor, rc.fechafinlabor, rc.estado, rc.facfin, u.nomusu, c.codigo, rc.facfin as secuencial, pe.establecimiento from recaudaxcaja rc join usuarios u on rc.idusuario_usuarios = u.idusuario join cajas c on rc.idcaja_cajas = c.idcaja join ptoemision pe on c.idptoemision_ptoemision = pe.idptoemision where rc.idusuario_usuarios = ?1 order by rc.idrecaudaxcaja desc limit 1", nativeQuery = true)
    LastConection_int getLastConection(Long idusuario);
    @Query(value = "select rc.idrecaudaxcaja, rc.fechainiciolabor, rc.fechafinlabor, rc.estado, rc.facfin, u.nomusu, c.codigo, rc.facfin as secuencial, pe.establecimiento, rc.idcaja_cajas as caja,  rc.idusuario_usuarios as usuario from recaudaxcaja rc join usuarios u on rc.idusuario_usuarios = u.idusuario join cajas c on rc.idcaja_cajas = c.idcaja join ptoemision pe on c.idptoemision_ptoemision = pe.idptoemision where u.nomusu = ?1 order by rc.idrecaudaxcaja desc limit 1", nativeQuery = true)
    LastConection_int getLConectionByUsername(String username);
    @Query(value = "select * from cajas c where c.idusuario_usuarios = ?1",nativeQuery = true)
    Cajas findByIdUsuario(Long idusuario);
    @Query(value = "select rc.idrecaudaxcaja, rc.fechainiciolabor, rc.fechafinlabor, rc.estado, rc.facfin, u.nomusu, c.codigo, rc.facfin as secuencial, pe.establecimiento, rc.idcaja_cajas as caja,  rc.idusuario_usuarios as usuario from recaudaxcaja rc join usuarios u on rc.idusuario_usuarios = u.idusuario join cajas c on rc.idcaja_cajas = c.idcaja join ptoemision pe on c.idptoemision_ptoemision = pe.idptoemision where u.nomusu = ?1 and u.codusu = ?2 order by rc.idrecaudaxcaja desc limit 1", nativeQuery = true)
    LastConection_int getLConectionByUserPass(String username, String password);
}
