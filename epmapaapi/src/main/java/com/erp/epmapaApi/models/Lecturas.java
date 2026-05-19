package com.erp.epmapaApi.models;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Date;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "lecturas")
public class Lecturas {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long idlectura;
    private Integer estado;
    private Date fechaemision;
    private Date fechalectura;
    private Float lecturaanterior;
    private Float lecturaactual;
    private String observaciones;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "idnovedad_novedades")
    private Novedad idnovedad_novedades;
    private Long idemision;
    private Long idabonado_abonados;
    private Long idfactura;
    private Long idcategoria;
}
