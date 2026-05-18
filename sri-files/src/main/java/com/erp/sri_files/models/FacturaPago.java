package com.erp.sri_files.models;

import java.math.BigDecimal;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Getter
@Setter
@Table(name = "fec_factura_pagos")
public class FacturaPago {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long idfacturapagos;
    
    @ManyToOne
    @JoinColumn(name = "idfactura")
    private Factura factura;
    
    private String formapago;
    private BigDecimal total;
    private Integer plazo;
    private String unidadtiempo;
    
    // Getters y Setters
}
