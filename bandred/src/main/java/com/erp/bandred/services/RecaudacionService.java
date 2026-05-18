package com.erp.bandred.services;

import com.erp.bandred.models.Recaudacion;
import com.erp.bandred.repositories.RecaudacionR;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class RecaudacionService {
    @Autowired
    private RecaudacionR dao;
    @Autowired
    private FormacobroService formacobroService;
    public Recaudacion save(Recaudacion r){
        Boolean fc = formacobroService.getValidationFc(r.getFormapago());
        System.out.println("true "+fc);
        //VALIDACION DE LA FORMA DE COBRO
        //SI SI EXISTE PUEDO CONTINUAR Y GUARDAR LA RECAUDACION CASO CONTRARIO RETORNAR NULLO
        try{
            System.out.println(r.getEstado());
            return dao.save(r);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
