package com.erp.epmapaApi.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
public class FacturaElectronicaDTO {
    private Long idfactura;
    private String nrofactura;
    private String razonsocialcomprador;
    private String emailcomprador;
    private LocalDateTime fechaemision;
    private String estado;
    private String xmlautorizado;
    private String referencia;
    private String direccioncomprador;
    private BigDecimal total;
    private Long idmodulo;
}
