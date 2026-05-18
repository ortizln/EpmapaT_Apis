package com.erp.bandred.interfaces;

import java.math.BigDecimal;
import java.time.LocalDate;

public interface Factura_int {
    Long getIdfactura();

    Long getIdmodulo();

    BigDecimal getTotal();

    Long getIdCliente();

    Long getIdAbonado();

    LocalDate getFeccrea();

    Long getFormaPago();

    Long getEstado();

    Long getPagado();

    Boolean getSwcondonar();

    LocalDate getfechacobro();
    Long getUsuariocobro();
    String getNrofactura();
    String getNombre();
    BigDecimal getIva();
    BigDecimal getInteres();
    String getDireccion();
}
