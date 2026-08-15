package com.erp.sri_files.models;

import jakarta.xml.bind.annotation.*;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "", propOrder = {"infoTributaria", "infoFactura", "detalles", "infoAdicional"})
@XmlRootElement(name = "factura")
public class Comprobante {

    @XmlAttribute(name = "id", required = true)
    private String id;

    @XmlAttribute(name = "version", required = true)
    private String version;

    @XmlElement(name = "infoTributaria", required = true)
    private InfoTributaria infoTributaria;

    @XmlElement(name = "infoFactura", required = true)
    private InfoFactura infoFactura;

    @XmlElementWrapper(name = "detalles")
    @XmlElement(name = "detalle", required = true)
    private List<Detalle> detalles = new ArrayList<>();

    // Si vas a emitir infoAdicional
    @XmlElementWrapper(name = "infoAdicional")
    @XmlElement(name = "campoAdicional")
    private List<CampoAdicional> infoAdicional = new ArrayList<>();

    @Data
    @XmlAccessorType(XmlAccessType.FIELD)
    public static class CampoAdicional {
        @XmlAttribute(name = "nombre", required = true)
        private String nombre;
        @XmlValue
        private String valor;
    }
}
