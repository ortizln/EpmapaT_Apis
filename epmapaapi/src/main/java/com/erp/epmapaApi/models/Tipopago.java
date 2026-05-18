package com.erp.epmapaApi.models;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "tipopago")
public class Tipopago {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long idtipopago;
    private String descripcion;
    private Long usucrea;
}
