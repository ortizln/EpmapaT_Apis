package com.erp.sri_files.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class AutorizacionSriResult {

    private boolean autorizado;
    private String xmlAutorizado;
    private String mensaje;
    private LocalDateTime fechaAutorizacion;
    private String numeroAutorizacion;


}
