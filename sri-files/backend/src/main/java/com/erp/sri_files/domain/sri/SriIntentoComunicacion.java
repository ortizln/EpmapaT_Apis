package com.erp.sri_files.domain.sri;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

/**
 * Bitácora de cada intento de comunicación con el SRI.
 * No guarda únicamente el último error: mantiene el historial completo,
 * incluida la meta del claim/concurrencia (worker_id + fecha_inicio).
 */
@Entity
@Table(name = "sri_intento_comunicacion")
public class SriIntentoComunicacion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "documento_id", nullable = false)
    private Long documentoId;

    @Column(name = "tipo_documento", length = 30, nullable = false)
    private String tipoDocumento;

    @Column(name = "clave_acceso", length = 49)
    private String claveAcceso;

    @Column(length = 20)
    private String ambiente;

    @Column(length = 30, nullable = false)
    private String servicio;

    @Column(columnDefinition = "text")
    private String endpoint;

    @Column(name = "numero_intento", nullable = false)
    private Integer numeroIntento;

    @Column(name = "fecha_inicio")
    private LocalDateTime fechaInicio;

    @Column(name = "fecha_fin")
    private LocalDateTime fechaFin;

    @Column(length = 40)
    private String resultado;

    @Column(name = "codigo_sri", length = 20)
    private String codigoSri;

    @Column(name = "mensaje_sri", columnDefinition = "text")
    private String mensajeSri;

    @Column(name = "informacion_adicional", columnDefinition = "text")
    private String informacionAdicional;

    @Column(name = "tipo_error", length = 50)
    private String tipoError;

    @Column(columnDefinition = "text")
    private String excepcion;

    @Column(name = "http_status")
    private Integer httpStatus;

    @Column(name = "duracion_ms")
    private Long duracionMs;

    @Column(nullable = false)
    private boolean reintentable;

    @Column(name = "requiere_consulta", nullable = false)
    private boolean requiereConsulta;

    @Column(name = "fecha_proximo_intento")
    private LocalDateTime fechaProximoIntento;

    @Column(name = "worker_id", length = 100)
    private String workerId;

    @Column(name = "correlation_id", length = 80)
    private String correlationId;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    public Long getId() {
        return id;
    }

    public Long getDocumentoId() {
        return documentoId;
    }

    public void setDocumentoId(Long documentoId) {
        this.documentoId = documentoId;
    }

    public String getTipoDocumento() {
        return tipoDocumento;
    }

    public void setTipoDocumento(String tipoDocumento) {
        this.tipoDocumento = tipoDocumento;
    }

    public String getClaveAcceso() {
        return claveAcceso;
    }

    public void setClaveAcceso(String claveAcceso) {
        this.claveAcceso = claveAcceso;
    }

    public String getAmbiente() {
        return ambiente;
    }

    public void setAmbiente(String ambiente) {
        this.ambiente = ambiente;
    }

    public String getServicio() {
        return servicio;
    }

    public void setServicio(String servicio) {
        this.servicio = servicio;
    }

    public String getEndpoint() {
        return endpoint;
    }

    public void setEndpoint(String endpoint) {
        this.endpoint = endpoint;
    }

    public Integer getNumeroIntento() {
        return numeroIntento;
    }

    public void setNumeroIntento(Integer numeroIntento) {
        this.numeroIntento = numeroIntento;
    }

    public LocalDateTime getFechaInicio() {
        return fechaInicio;
    }

    public void setFechaInicio(LocalDateTime fechaInicio) {
        this.fechaInicio = fechaInicio;
    }

    public LocalDateTime getFechaFin() {
        return fechaFin;
    }

    public void setFechaFin(LocalDateTime fechaFin) {
        this.fechaFin = fechaFin;
    }

    public String getResultado() {
        return resultado;
    }

    public void setResultado(String resultado) {
        this.resultado = resultado;
    }

    public String getCodigoSri() {
        return codigoSri;
    }

    public void setCodigoSri(String codigoSri) {
        this.codigoSri = codigoSri;
    }

    public String getMensajeSri() {
        return mensajeSri;
    }

    public void setMensajeSri(String mensajeSri) {
        this.mensajeSri = mensajeSri;
    }

    public String getInformacionAdicional() {
        return informacionAdicional;
    }

    public void setInformacionAdicional(String informacionAdicional) {
        this.informacionAdicional = informacionAdicional;
    }

    public String getTipoError() {
        return tipoError;
    }

    public void setTipoError(String tipoError) {
        this.tipoError = tipoError;
    }

    public String getExcepcion() {
        return excepcion;
    }

    public void setExcepcion(String excepcion) {
        this.excepcion = excepcion;
    }

    public Integer getHttpStatus() {
        return httpStatus;
    }

    public void setHttpStatus(Integer httpStatus) {
        this.httpStatus = httpStatus;
    }

    public Long getDuracionMs() {
        return duracionMs;
    }

    public void setDuracionMs(Long duracionMs) {
        this.duracionMs = duracionMs;
    }

    public boolean isReintentable() {
        return reintentable;
    }

    public void setReintentable(boolean reintentable) {
        this.reintentable = reintentable;
    }

    public boolean isRequiereConsulta() {
        return requiereConsulta;
    }

    public void setRequiereConsulta(boolean requiereConsulta) {
        this.requiereConsulta = requiereConsulta;
    }

    public LocalDateTime getFechaProximoIntento() {
        return fechaProximoIntento;
    }

    public void setFechaProximoIntento(LocalDateTime fechaProximoIntento) {
        this.fechaProximoIntento = fechaProximoIntento;
    }

    public String getWorkerId() {
        return workerId;
    }

    public void setWorkerId(String workerId) {
        this.workerId = workerId;
    }

    public String getCorrelationId() {
        return correlationId;
    }

    public void setCorrelationId(String correlationId) {
        this.correlationId = correlationId;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}