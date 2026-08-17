package com.erp.sri_files.domain.auth;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "usuario_auditoria")
public class UsuarioAuditoria {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "usuario_id", nullable = false)
    private UsuarioSistema usuario;

    @Column(name = "actor_uuid")
    private UUID actorUuid;

    @Column(name = "actor_username", length = 80)
    private String actorUsername;

    @Column(nullable = false, length = 40)
    private String accion;

    @Column(nullable = false, columnDefinition = "text")
    private String descripcion;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    public void setUsuario(UsuarioSistema usuario) {
        this.usuario = usuario;
    }

    public void setActorUuid(UUID actorUuid) {
        this.actorUuid = actorUuid;
    }

    public void setActorUsername(String actorUsername) {
        this.actorUsername = actorUsername;
    }

    public void setAccion(String accion) {
        this.accion = accion;
    }

    public void setDescripcion(String descripcion) {
        this.descripcion = descripcion;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public UsuarioSistema getUsuario() {
        return usuario;
    }

    public Long getId() {
        return id;
    }

    public String getAccion() {
        return accion;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public String getActorUsername() {
        return actorUsername;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}
