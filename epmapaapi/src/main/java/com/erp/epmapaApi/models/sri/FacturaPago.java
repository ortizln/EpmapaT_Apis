package com.erp.epmapaApi.models.sri;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "fec_factura_pagos")
public class FacturaPago {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long idfacturapagos;

    @ManyToOne
    @JoinColumn(name = "idfactura")
    private FecFactura factura;

    private String formapago;
    private BigDecimal total;
    private Integer plazo;
    private String unidadtiempo;
}
