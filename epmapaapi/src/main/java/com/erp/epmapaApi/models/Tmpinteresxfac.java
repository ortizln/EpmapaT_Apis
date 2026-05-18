package com.erp.epmapaApi.models;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "tmpinteresxfac")
public class Tmpinteresxfac {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long idtmpinteresxfac;
    private Long idfactura;
    private BigDecimal interesapagar;
    private LocalDateTime feccorte;
}