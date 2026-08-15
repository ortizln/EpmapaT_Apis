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
@Table(name = "documento_estado_historial")
public class DocumentoEstadoHistorial {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "documento_id", nullable = false)
    private DocumentoElectronico documento;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado_anterior", length = 40)
    private DocumentoEstado estadoAnterior;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado_nuevo", nullable = false, length = 40)
    private DocumentoEstado estadoNuevo;

    @Column(columnDefinition = "text")
    private String descripcion;

    @Column(length = 30)
    private String origen;

    @Column(name = "usuario_id")
    private Long usuarioId;

    @Column(columnDefinition = "jsonb")
    private String metadata;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    public Long getId() {
        return id;
    }

    public DocumentoEstado getEstadoAnterior() {
        return estadoAnterior;
    }

    public DocumentoEstado getEstadoNuevo() {
        return estadoNuevo;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public String getOrigen() {
        return origen;
    }

    public Long getUsuarioId() {
        return usuarioId;
    }

    public String getMetadata() {
        return metadata;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setDocumento(DocumentoElectronico documento) {
        this.documento = documento;
    }

    public void setEstadoAnterior(DocumentoEstado estadoAnterior) {
        this.estadoAnterior = estadoAnterior;
    }

    public void setEstadoNuevo(DocumentoEstado estadoNuevo) {
        this.estadoNuevo = estadoNuevo;
    }

    public void setDescripcion(String descripcion) {
        this.descripcion = descripcion;
    }

    public void setOrigen(String origen) {
        this.origen = origen;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
