package com.erp.sri_files.retenciones.dto;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
@Getter
@Setter
@Data
public class ImpuestoRet {
    public String codigo;
    public String codigoRetencion;
    public BigDecimal baseImponible;
    public BigDecimal porcentajeRetener;
    public BigDecimal valorRetenido;
    public String codDocSustento;
    public String numDocSustento;
    public String fechaEmisionDocSustento;
}
