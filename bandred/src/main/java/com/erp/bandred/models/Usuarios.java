package com.erp.bandred.models;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.time.ZonedDateTime;

@Entity
@Getter
@Setter
@NoArgsConstructor
@Table(name = "usuarios")
public class Usuarios {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long idusuario;
    private String identificausu;
    private String codusu;
    private String nomusu;
    private String email;
    private Boolean estado;
    @Column(name = "fdesde")
    private LocalDate fdesde;
    @Column(name = "fhasta")
    private LocalDate fhasta;
    @Column(name = "feccrea")
    private ZonedDateTime feccrea;
    private Long usumodi;
    @Column(name = "fecmodi")
    private ZonedDateTime fecmodi;
    private Boolean otrapestania;
    private String alias;
    private String priusu;
    private String perfil;
}