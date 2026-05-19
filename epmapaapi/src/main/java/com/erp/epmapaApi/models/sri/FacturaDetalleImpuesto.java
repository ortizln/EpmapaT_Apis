package com.erp.epmapaApi.models.sri;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Entity
@Getter
@Setter
@Table(name = "fec_factura_detalles_impuestos")
public class FacturaDetalleImpuesto {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long idfacturadetalleimpuestos;

    @ManyToOne
    @JoinColumn(name = "idfacturadetalle")
    private FacturaDetalle detalle;

    private String codigoimpuesto;
    private String codigoporcentaje;
    private BigDecimal baseimponible;
}
