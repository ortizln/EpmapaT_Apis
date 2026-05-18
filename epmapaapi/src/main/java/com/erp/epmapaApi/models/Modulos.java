package com.erp.epmapaApi.models;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;
@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
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
    @Temporal(TemporalType.DATE)
    @DateTimeFormat(iso= DateTimeFormat.ISO.DATE)
    @Column(name ="feccrea")
    private LocalDate feccrea;
    private Long usumodi;
    @Temporal(TemporalType.DATE)
    @DateTimeFormat(iso= DateTimeFormat.ISO.DATE)
    @Column(name ="fecmodi")
    private LocalDate fecmodi;
}
