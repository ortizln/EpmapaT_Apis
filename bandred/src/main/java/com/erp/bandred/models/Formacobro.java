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
@Table(name = "formacobro")
public class Formacobro {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long idformacobro;
    private String descripcion;
    private Boolean estado;
    private Long usumodi;
    private LocalDate fecmodi;
}
