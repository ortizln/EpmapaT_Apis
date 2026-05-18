package com.erp.epmapaApi.Interfaces;

import java.math.BigDecimal;
import java.time.LocalDate;

public interface FacturasSinCobroInter {
    Long getIdfactura();
    BigDecimal getSubtotal();
    Long getFormapago();
    LocalDate getFeccrea();
    LocalDate getFechatransferencia();
    String getNombre();
    String getCedula();
    String getDireccion();
    BigDecimal getInteres();
}
