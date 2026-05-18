package com.erp.bandred.DTO;

import lombok.*;

import java.sql.Timestamp;

@Data
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class FacturaRequestDTO {
    //private List<Long> facturas;
    private String autentification;
    private RecaudacionDTO recaudacion;
    private Long cuenta;
    private String secuencial;
    private Timestamp fechacompensacion;
}
