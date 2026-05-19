package com.erp.epmapaApi.models.sri;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Date;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "tabla15")
public class Tabla15 {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long idtabla15;
    private String codtabla15;
    private String nomtabla15;
}
