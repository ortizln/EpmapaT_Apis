package com.erp.bandred.models;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Getter
@Setter
@NoArgsConstructor
@Table(name = "impuestos")
public class Impuestos {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long idimpuesto;
    private Integer codigo;
    private String descripcion;
    private Integer valor;

}