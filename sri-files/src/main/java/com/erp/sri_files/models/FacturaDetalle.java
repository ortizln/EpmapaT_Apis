package com.erp.sri_files.models;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "fec_factura_detalles")
public class FacturaDetalle {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long idfacturadetalle;
    
    @ManyToOne
    @JoinColumn(name = "idfactura")
    private Factura factura;
    
    private String codigoprincipal;
    private String descripcion;
    private BigDecimal cantidad;
    private BigDecimal preciounitario;
    private BigDecimal descuento;
    
    @OneToMany(mappedBy = "detalle", cascade = CascadeType.ALL)
    private List<FacturaDetalleImpuesto> impuestos = new ArrayList<>();

    
    // Getters y Setters
}