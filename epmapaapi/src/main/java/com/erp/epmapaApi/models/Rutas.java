package com.erp.epmapaApi.models;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.time.LocalDate;

@Entity
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Table(name="rutas")
public class Rutas {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long idruta;
    private String descripcion;
    private Long orden;
    private String codigo;
    private Long usucrea;
    private LocalDate feccrea;
    private Long usumodi;
    private LocalDate fecmodi;
    private Boolean estado;
}
