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
@Table(name ="personeriajuridica")
public class PersonJuridica {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long idpjuridica;
    private String descripcion;
}
