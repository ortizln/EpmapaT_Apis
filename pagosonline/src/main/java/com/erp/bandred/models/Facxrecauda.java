package com.erp.bandred.models;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

@Entity
@Getter
@Setter
@NoArgsConstructor
@Table(name= "facxrecauda")
public class Facxrecauda {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long idfacxrecauda;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "idrecaudacion")
    private Recaudacion idrecaudacion;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "idfactura")
    private Facturas idfactura;
    private Long estado;
    private LocalDate fechaeliminacion;
    private Long idusuarioeliminacion;
}