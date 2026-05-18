package com.erp.bandred.models;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Entity
@Getter
@Setter
@NoArgsConstructor
@Table(name="rubroxfac")
public class Rubroxfac {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long idrubroxfac;
    private BigDecimal cantidad;
    private BigDecimal valorunitario;
    private Long estado;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "idfactura_facturas")
    private Facturas idfactura_facturas;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "idrubro_rubros")
    private Rubros idrubro_rubros;
}