package com.erp.bandred.DTO;

import lombok.Data;

import java.time.LocalDate;
import java.time.LocalTime;

@Data
public class ReportdataDTO {
    Long idusuario;
    LocalDate df, hf;
    LocalTime dh, hh;
}
