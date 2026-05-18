package com.erp.sri_files.models;

import java.math.BigDecimal;


import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

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
