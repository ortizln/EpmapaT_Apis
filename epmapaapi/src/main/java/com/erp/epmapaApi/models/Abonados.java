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
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "abonados")
public class Abonados {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long idabonado;
    private String nromedidor;
    private Long lecturainicial;
    private Long estado;
    private LocalDate fechainstalacion;
    private String marca;
    private Long secuencia;
    private String direccionubicacion;
    private String localizacion;
    private String observacion;
    private String departamento;
    private String piso;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "idresponsable")
    private Clientes idresponsable;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "idcategoria_categorias")
    private Categorias idcategoria_categorias;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "idruta_rutas")
    private Rutas idruta_rutas;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "idcliente_clientes")
    private Clientes idcliente_clientes;
    @ManyToOne
    @JoinColumn(name = "idubicacionm_ubicacionm")
    private Ubicacionm idubicacionm_ubicacionm;
    @ManyToOne
    @JoinColumn(name = "idtipopago_tipopago")
    private Tipopago idtipopago_tipopago;
    @ManyToOne
    @JoinColumn(name = "idestadom_estadom")
    private Estadom idestadom_estadom;
    private Long medidorprincipal;
    private Long usucrea;
    private LocalDate feccrea;
    private Long usumodi;
    private LocalDate fecmodi;
    private Boolean adultomayor;
    private Boolean municipio;
    private Boolean swalcantarillado;
    private Long promedio;
    private String geolocalizacion;
}
