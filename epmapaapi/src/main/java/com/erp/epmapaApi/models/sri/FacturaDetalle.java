package com.erp.epmapaApi.models.sri;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "fec_factura_detalles")
public class FacturaDetalle {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long idfacturadetalle;

    @ManyToOne
    @JoinColumn(name = "idfactura")
    private FecFactura factura;

    private String codigoprincipal;
    private String descripcion;
    private BigDecimal cantidad;
    private BigDecimal preciounitario;
    private BigDecimal descuento;

    @OneToMany(mappedBy = "detalle", cascade = CascadeType.ALL)
    private List<FacturaDetalleImpuesto> impuestos = new ArrayList<>();
}
