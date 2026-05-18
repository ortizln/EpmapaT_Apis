package com.erp.sri_files.dto;

import lombok.Data;

import java.time.LocalDate;
@Data
public class FecFacturaDTO {
    private Long idfactura;

    private String claveacceso;
    private String secuencial;
    private String xmlautorizado;
    private String errores;
    private String estado;
    private String establecimiento;
    private String puntoemision;
    private String direccionestablecimiento;
    private LocalDate fechaemision;
    private String tipoidentificacioncomprador;
    private String identificacioncomprador;
    private String guiaremision;
    private String razonsocialcomprador;
    private String telefonocomprador;
    private String emailcomprador;
    private String concepto;
    private String recaudador;
    private String referencia;
    private String direccioncomprador;
}
