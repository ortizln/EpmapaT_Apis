package com.erp.epmapaApi.DTO;

import lombok.*;
import org.springframework.format.annotation.DateTimeFormat;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ValorFactDTO {
    private Long idfactura;
    private Float subtotal;
    private BigDecimal total;
    private BigDecimal interes;
    private Integer numfacturas;
    private Long cuenta;
    private String nombre;
    private String cedula;
    private String direccionubicacion;
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private LocalDate fectransferencia;
    private Long formapago;
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private LocalDate feccrea;
}
