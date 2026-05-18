package com.erp.sri_files.interfaces;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class fecFacturaDatos {
    private Long idfactura;
    private String xmlautorizado;
    private LocalDate fechaemision;
}
