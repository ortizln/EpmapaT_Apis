package com.erp.bandred.models;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
@Entity
@Getter
@Setter
@NoArgsConstructor
@Table(name = "rubros")
public class Rubros {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long idrubro;
    private String descripcion;
    private BigDecimal valor;
    private Long tipo;
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
    private Boolean swiva;
    private Boolean calculable;
    private Boolean estado;
}
