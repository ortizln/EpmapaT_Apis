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
@Table(name = "cajas")
public class Cajas {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long idcaja;
    private String descripcion;
    private String codigo;
    private Long estado;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name ="idptoemision_ptoemision")
    private Ptoemision idptoemision_ptoemision;
    private Long usucrea;
    private LocalDate feccrea;
    private Long usumodi;
    private LocalDate fecmodi;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name="idusuario_usuarios")
    private Usuarios idusuario_usuarios;
    private String ultimafact;
}
