package com.erp.sri_files.models;

import jakarta.xml.bind.annotation.*;
import lombok.Data;

@Data
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "", propOrder = {
        "ambiente","tipoEmision","razonSocial","nombreComercial","ruc","claveAcceso",
        "codDoc","estab","ptoEmi","secuencial","dirMatriz"
})
public class InfoTributaria {
    @XmlElement(required = true) private String ambiente;     // "1"/"2"
    @XmlElement(required = true) private String tipoEmision;  // "1"
    @XmlElement(required = true) private String razonSocial;
    private String nombreComercial;
    @XmlElement(required = true) private String ruc;
    @XmlElement(required = true) private String claveAcceso;
    @XmlElement(required = true) private String codDoc;
    @XmlElement(required = true) private String estab;
    @XmlElement(required = true) private String ptoEmi;
    @XmlElement(required = true) private String secuencial;
    @XmlElement(required = true) private String dirMatriz;
}
