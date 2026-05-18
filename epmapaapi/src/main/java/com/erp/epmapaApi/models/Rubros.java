package com.erp.epmapaApi.models;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.format.annotation.DateTimeFormat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Date;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "rubros")
public class Rubros {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long idrubro;
    private String descripcion;
    private Boolean estado;
    private Boolean calculable;
    private BigDecimal valor;
    private Boolean swiva;
    private Integer tipo;
    private Long esiva;
    private Long esdebito;
    private Long facturable;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "idmodulo_modulos")
    private Modulos idmodulo_modulos;
    private Long usucrea;
    private LocalDate feccrea;
    private Long usumodi;
    private LocalDate fecmodi;
}
