package com.erp.sri_files.domain.documento;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

@Entity
@Table(name = "documento_archivo")
public class DocumentoArchivo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "documento_id", nullable = false)
    private DocumentoElectronico documento;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_archivo", nullable = false, length = 30)
    private DocumentoArchivoTipo tipoArchivo;

    @Column(name = "nombre_archivo", nullable = false, length = 255)
    private String nombreArchivo;

    @Column(name = "mime_type", length = 100)
    private String mimeType;

    @Column(columnDefinition = "text")
    private String ruta;

    @Column(name = "hash_sha256", length = 64)
    private String hashSha256;

    @Column(name = "tamanio")
    private Long tamanio;

    @Column(nullable = false)
    private int version = 1;

    @Column(nullable = false)
    private boolean activo = true;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
}
