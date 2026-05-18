package com.erp.epmapaApi.models;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalTime;

@Entity
@Getter
@Setter
@Table(name = "facturas")
public class Facturas {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long idfactura;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "idmodulo")
    private Modulos idmodulo;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "idcliente")
    private Clientes idcliente;
    private String nrofactura;
    private Long porcexoneracion;
    private String razonexonera;
    private BigDecimal totaltarifa;
    private Integer pagado;
    private Long usuariocobro;
    private LocalDate fechacobro;
    private Long estado;
    private Long usuarioanulacion;
    private LocalDate fechaanulacion;
    private String razonanulacion;
    private Long usuarioeliminacion;
    private LocalDate fechaeliminacion;
    private String razoneliminacion;
    private Long conveniopago;
    private LocalDate fechaconvenio;
    private Long estadoconvenio;
    private Long formapago;
    private String refeformapago;
    @JsonFormat(pattern = "H:m:s")
    private LocalTime horacobro;
    private Long usuariotransferencia;
    private LocalDate fechatransferencia;
    private Long usucrea;
    private LocalDate feccrea;
    private Long usumodi;
    private LocalDate fecmodi;
    private BigDecimal valorbase;
    private Long idabonado;
    private BigDecimal interescobrado;
    private BigDecimal swiva;
    private Boolean swcondonar;
    private BigDecimal valornotacredito;
    private String secuencialfacilito;
    private Timestamp fechacompensacion;

}
