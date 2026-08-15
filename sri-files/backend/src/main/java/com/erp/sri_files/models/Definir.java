package com.erp.sri_files.models;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import org.hibernate.annotations.Type;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "definir")
public class Definir {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long iddefinir;
    private String razonsocial;
    private String nombrecomercial;
    private String ruc;
    private String direccion;
    private Byte tipoambiente;
    private float iva;
    private String empresa;
    private String ubirepo;
    private String posiacti;
    private String longacti;
    private String naturaleza;
    private LocalDate fechap;
    private String nombre;
    private String ubicomprobantes;
    private String asunto;
    private String textomail;
    private String dirmatriz;
    private LocalDate fechacierre;
    private String f_i;
    private String f_g;
    private BigDecimal porciva;
    private String ciudad;
    private Long idtabla17;
    private String ubidigi;
    private String ubimagenes;
    private String swpreingsin;
   // @Type(type = "org.hibernate.type.BinaryType")
   //@Lob @Basic(fetch = FetchType.LAZY)
   private byte[] firma;
    private String clave_firma;
    private String email;
    private String clave_email;

}