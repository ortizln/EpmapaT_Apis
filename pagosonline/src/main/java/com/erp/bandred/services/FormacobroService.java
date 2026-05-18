package com.erp.bandred.services;

import com.erp.bandred.repositories.FormacobroR;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class FormacobroService {
    @Autowired
    private FormacobroR fc_dao;
    public Boolean getValidationFc(Long idformacobro){
        return fc_dao.valFormaCobro(idformacobro) == null;
    }
}
