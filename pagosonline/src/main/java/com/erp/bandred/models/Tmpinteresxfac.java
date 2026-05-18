package com.erp.bandred.models;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Table(name= "tmpinteresxfac")
public class Tmpinteresxfac {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long idtmpinteresxfac;
    private Long idfactura;
    private BigDecimal interesapagar;
    private LocalDate feccorte;

}
