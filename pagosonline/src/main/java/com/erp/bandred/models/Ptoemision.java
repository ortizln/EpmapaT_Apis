package com.erp.bandred.models;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;

@Entity
@Getter
@Setter
@NoArgsConstructor
@Table(name = "ptoemision")
public class Ptoemision {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long idptoemision;
    private String establecimiento;
    private String direccion;
    private Long estado;
    private String telefono;
    private String nroautorizacion;
    private Long usucrea;
    private LocalDate feccrea;
}
