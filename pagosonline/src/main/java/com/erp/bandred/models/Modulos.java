package com.erp.bandred.models;

import java.time.LocalDate;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Getter @Setter @NoArgsConstructor
@Table(name = "modulos")
public class Modulos {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long idmodulo;
    private String descripcion;
    private Long estado;
    private Long periodicidad;
    private Long conveniospago;
    private Long usucrea;
    private LocalDate feccrea;
    private Long usumodi;
    private LocalDate fecmodi;

}