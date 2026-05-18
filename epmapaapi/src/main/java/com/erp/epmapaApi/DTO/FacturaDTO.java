package com.erp.epmapaApi.DTO;

import lombok.*;

import java.math.BigDecimal;
import java.util.List;

@Data
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FacturaDTO {
    private Long cuenta;
    private BigDecimal total;
    private String responsablepago;
    private List<Long> facturas;
    private String cedula;
    private String direccion;

}