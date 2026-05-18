package com.erp.bandred.models;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@NoArgsConstructor
@Table(name="recaudacion")
public class Recaudacion {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long idrecaudacion;
    private LocalDateTime fechacobro;
    private Long recaudador;
    private BigDecimal totalpagar;
    private BigDecimal recibo;
    private BigDecimal cambio;
    private Long formapago;
    private BigDecimal valor;
    private Long estado;
    private LocalDate fechaeliminacion;
    private Long usuarioeliminacion;
    private String observaciones;
    private Long ncnumero;
    private BigDecimal ncvalor;
    private Long usucrea;
    private LocalDateTime feccrea;
}