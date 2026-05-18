package com.erp.bandred.DTO;

import lombok.*;

import java.math.BigDecimal;
@Data
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class RecaudacionDTO {
    private BigDecimal totalpagar;
    private BigDecimal recibo; // no obligatorio
    private BigDecimal cambio; // no obligatorio
    private Long formapago; // 1 por defecto
    private BigDecimal valor;//no obligatorio

}
