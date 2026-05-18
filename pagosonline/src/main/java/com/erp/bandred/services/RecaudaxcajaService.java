package com.erp.bandred.services;

import com.erp.bandred.models.Cajas;
import com.erp.bandred.models.Recaudaxcaja;
import com.erp.bandred.models.Usuarios;
import com.erp.bandred.repositories.CajasR;
import com.erp.bandred.repositories.RecaudaxcajaR;
import com.erp.bandred.repositories.UsuariosR;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.HashMap;
import java.util.Map;

@Service
public class RecaudaxcajaService {
    @Autowired
    private RecaudaxcajaR rc_dao;
    @Autowired
    private CajasR c_dao;
    @Autowired
    private UsuariosR u_dao;
    public void abrirCaja(Long idrecaudaxcaja, Long idusuario){
        Recaudaxcaja _rxc = rc_dao.findById(idrecaudaxcaja).orElseThrow();
        Recaudaxcaja n_rxc = new Recaudaxcaja();
        Usuarios _usuario = new Usuarios();
        Cajas _caja = c_dao.findByIdUsuario(idusuario);
        LocalDate today = LocalDate.now();
        LocalTime justNow = LocalTime.now();
        _usuario.setIdusuario(idusuario);
        n_rxc.setFacinicio(_rxc.getFacfin());
        n_rxc.setFacfin(_rxc.getFacfin());
        n_rxc.setEstado(1);
        n_rxc.setIdusuario_usuarios(_usuario);
        n_rxc.setIdcaja_cajas(_caja);
        n_rxc.setFechainiciolabor(today);
        n_rxc.setHorainicio(justNow);
        rc_dao.save(n_rxc);
    }
    public <S extends Recaudaxcaja> S save(S recaudaxcaja){
        return rc_dao.save(recaudaxcaja);
    }
    public Object cerrarCaja(Long idrxc){
        Map<String, Object> respuesta = new HashMap<>();
        //CORREGIR ESTE DESORDEN OJO
        Recaudaxcaja rxc = rc_dao.findByIdRecaudaxcaja(idrxc);
        Recaudaxcaja _rxc = rc_dao.findById(rxc.getIdrecaudaxcaja()).orElseThrow();
        if(rxc.getFacfin()== null){
            _rxc.setFacfin(rxc.getFacinicio());
        }else{
            _rxc.setFacfin(rxc.getFacfin());
        }
        LocalDate today = LocalDate.now();
        LocalTime jusNow = LocalTime.now();
        _rxc.setFacinicio(rxc.getFacinicio());
        _rxc.setFacfin(rxc.getFacfin());
        _rxc.setFechainiciolabor(rxc.getFechainiciolabor());
        _rxc.setIdcaja_cajas(rxc.getIdcaja_cajas());
        _rxc.setIdusuario_usuarios(rxc.getIdusuario_usuarios());
        _rxc.setEstado(0);
        _rxc.setHorafin(jusNow);
        _rxc.setFechafinlabor(today);
        rc_dao.save(_rxc);
        respuesta.put("Mensaje:", "Caja cerrada el dia: "+today + " a la hora: "+ jusNow);
        return respuesta;

    }
    public void updateLastfactFinFac(Long idusuario, Long secuencial){
        Recaudaxcaja lastConection = rc_dao.findLastConectionAll(idusuario);
        Recaudaxcaja rxc = new Recaudaxcaja();
        rxc.setIdrecaudaxcaja(lastConection.getIdrecaudaxcaja());
        rxc.setEstado(lastConection.getEstado());
        rxc.setFacinicio(lastConection.getFacinicio());
        rxc.setIdcaja_cajas(lastConection.getIdcaja_cajas());
        rxc.setIdusuario_usuarios(lastConection.getIdusuario_usuarios());
        rxc.setFacfin(secuencial);
        rxc.setFechainiciolabor(lastConection.getFechainiciolabor());
        rxc.setHorainicio(lastConection.getHorainicio());
        rc_dao.save(rxc);
    }
}
