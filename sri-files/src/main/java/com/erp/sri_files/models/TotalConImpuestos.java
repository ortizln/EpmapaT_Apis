package com.erp.sri_files.models;

import jakarta.xml.bind.annotation.*;
import lombok.Data;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Data
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "", propOrder = {"totalImpuestos"})
public class TotalConImpuestos {

    @XmlElement(name = "totalImpuesto", required = true)
    private List<TotalImpuesto> totalImpuestos = new ArrayList<>();

    @Data
    @XmlAccessorType(XmlAccessType.FIELD)
    @XmlType(name = "", propOrder = {"codigo","codigoPorcentaje","baseImponible","valor"})
    public static class TotalImpuesto {
        @XmlElement(required = true) private String codigo;
        @XmlElement(required = true) private String codigoPorcentaje;
        @XmlElement(required = true) private BigDecimal baseImponible;
        @XmlElement(required = true) private BigDecimal valor;
    }
}
