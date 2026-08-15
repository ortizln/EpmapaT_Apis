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
@Table(name = "documento_error")
public class DocumentoError {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "documento_id", nullable = false)
    private DocumentoElectronico documento;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private DocumentoEtapa etapa;

    @Column(length = 100)
    private String codigo;

    @Column(nullable = false, columnDefinition = "text")
    private String mensaje;

    @Column(columnDefinition = "text")
    private String detalle;

    @Column(name = "stack_trace", columnDefinition = "text")
    private String stackTrace;

    @Column(nullable = false)
    private boolean recuperable;

    @Column(nullable = false)
    private boolean resuelto;

    @Column(name = "resuelto_por")
    private Long resueltoPor;

    @Column(name = "fecha_resolucion")
    private LocalDateTime fechaResolucion;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    public Long getId() {
        return id;
    }

    public DocumentoElectronico getDocumento() {
        return documento;
    }

    public void setDocumento(DocumentoElectronico documento) {
        this.documento = documento;
    }

    public void setEtapa(DocumentoEtapa etapa) {
        this.etapa = etapa;
    }

    public void setCodigo(String codigo) {
        this.codigo = codigo;
    }

    public void setMensaje(String mensaje) {
        this.mensaje = mensaje;
    }

    public void setDetalle(String detalle) {
        this.detalle = detalle;
    }

    public void setStackTrace(String stackTrace) {
        this.stackTrace = stackTrace;
    }

    public void setRecuperable(boolean recuperable) {
        this.recuperable = recuperable;
    }

    public void setResuelto(boolean resuelto) {
        this.resuelto = resuelto;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public DocumentoEtapa getEtapa() {
        return etapa;
    }

    public String getCodigo() {
        return codigo;
    }

    public String getMensaje() {
        return mensaje;
    }

    public String getDetalle() {
        return detalle;
    }

    public boolean isRecuperable() {
        return recuperable;
    }

    public boolean isResuelto() {
        return resuelto;
    }

    public LocalDateTime getFechaResolucion() {
        return fechaResolucion;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}
