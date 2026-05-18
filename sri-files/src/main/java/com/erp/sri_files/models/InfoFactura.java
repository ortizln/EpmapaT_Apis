package com.erp.sri_files.models;

import jakarta.xml.bind.annotation.*;
import lombok.Data;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Data
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "", propOrder = {
        "fechaEmision","obligadoContabilidad","tipoIdentificacionComprador","razonSocialComprador",
        "identificacionComprador","direccionComprador","contribuyenteEspecial",
        "totalSinImpuestos","totalDescuento","totalConImpuestos",
        "propina","importeTotal","moneda","pagos"
})
public class InfoFactura {

    @XmlElement(required = true) private String fechaEmision; // dd/MM/yyyy
    private String obligadoContabilidad;
    @XmlElement(required = true) private String tipoIdentificacionComprador;
    @XmlElement(required = true) private String razonSocialComprador;
    @XmlElement(required = true) private String identificacionComprador;
    private String direccionComprador;
    private String contribuyenteEspecial;

    @XmlElement(required = true) private BigDecimal totalSinImpuestos;
    @XmlElement(required = true) private BigDecimal totalDescuento;

    @XmlElement(required = true) private TotalConImpuestos totalConImpuestos;

    @XmlElement(required = true) private BigDecimal propina = BigDecimal.ZERO;
    @XmlElement(required = true) private BigDecimal importeTotal;
    @XmlElement(required = true) private String moneda = "DOLAR";

    @XmlElementWrapper(name = "pagos")
    @XmlElement(name = "pago")
    private List<Pago> pagos = new ArrayList<>();

    @Data
    @XmlAccessorType(XmlAccessType.FIELD)
    @XmlType(name = "", propOrder = {"formaPago","total","plazo","unidadTiempo"})
    public static class Pago {
        @XmlElement(required = true) private String formaPago;
        @XmlElement(required = true) private BigDecimal total;
        private String plazo;
        private String unidadTiempo;
    }
}
