package com.erp.epmapaApi.models;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

@Entity
@Setter
@Getter
@AllArgsConstructor
@NoArgsConstructor
@Table(name ="clientes")
public class Clientes {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long idcliente;
    private String cedula;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "idtpidentifica_tpidentifica")
    private Tpidentifica idtpidentifica_tpidentifica;
    private String nombre;
    private String direccion;
    private String telefono;
    private LocalDate fechanacimiento;
    private Long discapacitado;
    private Long porcdiscapacidad;
    private Long porcexonera;
    private Long estado;
    private String email;
    private Long usucrea;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name ="idnacionalidad_nacionalidad")
    private Nacionalidad idnacionalidad_nacionalidad;
    private LocalDate feccrea;
    private Long usumodi;
    private LocalDate fecmodi;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name ="idpjuridica_personeriajuridica")
    private PersonJuridica idpjuridica_personeriajuridica;
    private String username;
    private String password;
    private boolean activo;
    private String rol;
}
