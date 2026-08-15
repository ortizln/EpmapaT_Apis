package com.erp.sri_files.models;

import jakarta.xml.bind.annotation.*;
import lombok.Data;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Data
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "", propOrder = {
        "codigoPrincipal","codigoAuxiliar","descripcion","cantidad","precioUnitario","descuento",
        "precioTotalSinImpuesto","detallesAdicionales","impuestos"
})
public class Detalle {

    @XmlElement(required = true) private String codigoPrincipal;
    private String codigoAuxiliar;
    @XmlElement(required = true) private String descripcion;
    @XmlElement(required = true) private BigDecimal cantidad;
    @XmlElement(required = true) private BigDecimal precioUnitario;
    @XmlElement(required = true) private BigDecimal descuento;
    @XmlElement(required = true) private BigDecimal precioTotalSinImpuesto;

    @XmlElementWrapper(name = "detallesAdicionales")
    @XmlElement(name = "detAdicional")
    private List<DetalleAdicional> detallesAdicionales = new ArrayList<>();

    @XmlElementWrapper(name = "impuestos")
    @XmlElement(name = "impuesto", required = true)
    private List<Impuesto> impuestos = new ArrayList<>();

    @Data
    @XmlAccessorType(XmlAccessType.FIELD)
    public static class DetalleAdicional {
        @XmlAttribute(name = "nombre", required = true)
        private String nombre;
        @XmlValue
        private String valor;
    }

    @Data
    @XmlAccessorType(XmlAccessType.FIELD)
    @XmlType(name = "", propOrder = {"codigo","codigoPorcentaje","tarifa","baseImponible","valor"})
    public static class Impuesto {
        @XmlElement(required = true) private String codigo;
        @XmlElement(required = true) private String codigoPorcentaje;
        private BigDecimal tarifa; // opcional
        @XmlElement(required = true) private BigDecimal baseImponible;
        @XmlElement(required = true) private BigDecimal valor;
    }
}
