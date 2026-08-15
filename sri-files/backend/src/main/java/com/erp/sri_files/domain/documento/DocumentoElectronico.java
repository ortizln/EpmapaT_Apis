package com.erp.sri_files.domain.documento;

import com.erp.sri_files.domain.common.AuditableEntity;
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

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "documento_electronico")
public class DocumentoElectronico extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private UUID uuid;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "empresa_id", nullable = false)
    private Empresa empresa;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "establecimiento_id")
    private Establecimiento establecimientoRelacion;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "punto_emision_id")
    private PuntoEmision puntoEmisionRelacion;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_documento", nullable = false, length = 30)
    private TipoDocumento tipoDocumento;

    @Column(nullable = false)
    private short ambiente;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado_actual", nullable = false, length = 40)
    private DocumentoEstado estadoActual;

    @Column(name = "external_id", length = 150)
    private String externalId;

    @Column(name = "idempotency_key", length = 200)
    private String idempotencyKey;

    @Column(name = "codigo_documento", length = 2)
    private String codigoDocumento;

    @Column(length = 3)
    private String establecimiento;

    @Column(name = "punto_emision", length = 3)
    private String puntoEmision;

    @Column(length = 9)
    private String secuencial;

    @Column(name = "numero_documento", length = 30)
    private String numeroDocumento;

    @Column(name = "clave_acceso", length = 49)
    private String claveAcceso;

    @Column(name = "fecha_emision", nullable = false)
    private LocalDate fechaEmision;

    @Column(name = "identificacion_receptor", length = 20)
    private String identificacionReceptor;

    @Column(name = "razon_social_receptor", length = 300)
    private String razonSocialReceptor;

    @Column(name = "email_receptor", length = 320)
    private String emailReceptor;

    @Column(length = 10)
    private String moneda;

    @Column(precision = 18, scale = 6)
    private BigDecimal subtotal;

    @Column(precision = 18, scale = 6)
    private BigDecimal descuento;

    @Column(precision = 18, scale = 6)
    private BigDecimal impuestos;

    @Column(precision = 18, scale = 6)
    private BigDecimal total;

    @Column(name = "json_original", nullable = false, columnDefinition = "jsonb")
    private String jsonOriginal;

    @Column(name = "numero_autorizacion", length = 100)
    private String numeroAutorizacion;

    @Column(name = "fecha_autorizacion")
    private LocalDateTime fechaAutorizacion;

    @Column(name = "mensaje_sri", columnDefinition = "text")
    private String mensajeSri;

    @Column(name = "requiere_intervencion", nullable = false)
    private boolean requiereIntervencion;

    @Column(name = "intentos_procesamiento", nullable = false)
    private int intentosProcesamiento;

    @Column(name = "fecha_recepcion", nullable = false)
    private LocalDateTime fechaRecepcion;

    @Column(name = "fecha_inicio_procesamiento")
    private LocalDateTime fechaInicioProcesamiento;

    @Column(name = "fecha_finalizacion")
    private LocalDateTime fechaFinalizacion;

    public Long getId() {
        return id;
    }

    public UUID getUuid() {
        return uuid;
    }

    public void setUuid(UUID uuid) {
        this.uuid = uuid;
    }

    public Empresa getEmpresa() {
        return empresa;
    }

    public void setEmpresa(Empresa empresa) {
        this.empresa = empresa;
    }

    public TipoDocumento getTipoDocumento() {
        return tipoDocumento;
    }

    public void setTipoDocumento(TipoDocumento tipoDocumento) {
        this.tipoDocumento = tipoDocumento;
    }

    public short getAmbiente() {
        return ambiente;
    }

    public void setAmbiente(short ambiente) {
        this.ambiente = ambiente;
    }

    public DocumentoEstado getEstadoActual() {
        return estadoActual;
    }

    public void setEstadoActual(DocumentoEstado estadoActual) {
        this.estadoActual = estadoActual;
    }

    public String getExternalId() {
        return externalId;
    }

    public void setExternalId(String externalId) {
        this.externalId = externalId;
    }

    public String getIdempotencyKey() {
        return idempotencyKey;
    }

    public void setIdempotencyKey(String idempotencyKey) {
        this.idempotencyKey = idempotencyKey;
    }

    public String getCodigoDocumento() {
        return codigoDocumento;
    }

    public void setCodigoDocumento(String codigoDocumento) {
        this.codigoDocumento = codigoDocumento;
    }

    public String getEstablecimiento() {
        return establecimiento;
    }

    public void setEstablecimiento(String establecimiento) {
        this.establecimiento = establecimiento;
    }

    public String getPuntoEmision() {
        return puntoEmision;
    }

    public void setPuntoEmision(String puntoEmision) {
        this.puntoEmision = puntoEmision;
    }

    public String getSecuencial() {
        return secuencial;
    }

    public void setSecuencial(String secuencial) {
        this.secuencial = secuencial;
    }

    public String getNumeroDocumento() {
        return numeroDocumento;
    }

    public void setNumeroDocumento(String numeroDocumento) {
        this.numeroDocumento = numeroDocumento;
    }

    public String getClaveAcceso() {
        return claveAcceso;
    }

    public void setClaveAcceso(String claveAcceso) {
        this.claveAcceso = claveAcceso;
    }

    public LocalDate getFechaEmision() {
        return fechaEmision;
    }

    public void setFechaEmision(LocalDate fechaEmision) {
        this.fechaEmision = fechaEmision;
    }

    public String getIdentificacionReceptor() {
        return identificacionReceptor;
    }

    public void setIdentificacionReceptor(String identificacionReceptor) {
        this.identificacionReceptor = identificacionReceptor;
    }

    public String getRazonSocialReceptor() {
        return razonSocialReceptor;
    }

    public void setRazonSocialReceptor(String razonSocialReceptor) {
        this.razonSocialReceptor = razonSocialReceptor;
    }

    public String getEmailReceptor() {
        return emailReceptor;
    }

    public void setEmailReceptor(String emailReceptor) {
        this.emailReceptor = emailReceptor;
    }

    public String getMoneda() {
        return moneda;
    }

    public void setMoneda(String moneda) {
        this.moneda = moneda;
    }

    public BigDecimal getSubtotal() {
        return subtotal;
    }

    public void setSubtotal(BigDecimal subtotal) {
        this.subtotal = subtotal;
    }

    public BigDecimal getDescuento() {
        return descuento;
    }

    public void setDescuento(BigDecimal descuento) {
        this.descuento = descuento;
    }

    public BigDecimal getImpuestos() {
        return impuestos;
    }

    public void setImpuestos(BigDecimal impuestos) {
        this.impuestos = impuestos;
    }

    public BigDecimal getTotal() {
        return total;
    }

    public void setTotal(BigDecimal total) {
        this.total = total;
    }

    public String getJsonOriginal() {
        return jsonOriginal;
    }

    public void setJsonOriginal(String jsonOriginal) {
        this.jsonOriginal = jsonOriginal;
    }

    public boolean isRequiereIntervencion() {
        return requiereIntervencion;
    }

    public void setRequiereIntervencion(boolean requiereIntervencion) {
        this.requiereIntervencion = requiereIntervencion;
    }

    public int getIntentosProcesamiento() {
        return intentosProcesamiento;
    }

    public void setIntentosProcesamiento(int intentosProcesamiento) {
        this.intentosProcesamiento = intentosProcesamiento;
    }

    public LocalDateTime getFechaRecepcion() {
        return fechaRecepcion;
    }

    public void setFechaRecepcion(LocalDateTime fechaRecepcion) {
        this.fechaRecepcion = fechaRecepcion;
    }

    public String getNumeroAutorizacion() {
        return numeroAutorizacion;
    }

    public void setNumeroAutorizacion(String numeroAutorizacion) {
        this.numeroAutorizacion = numeroAutorizacion;
    }

    public LocalDateTime getFechaAutorizacion() {
        return fechaAutorizacion;
    }

    public void setFechaAutorizacion(LocalDateTime fechaAutorizacion) {
        this.fechaAutorizacion = fechaAutorizacion;
    }

    public String getMensajeSri() {
        return mensajeSri;
    }

    public void setMensajeSri(String mensajeSri) {
        this.mensajeSri = mensajeSri;
    }

    public LocalDateTime getFechaInicioProcesamiento() {
        return fechaInicioProcesamiento;
    }

    public void setFechaInicioProcesamiento(LocalDateTime fechaInicioProcesamiento) {
        this.fechaInicioProcesamiento = fechaInicioProcesamiento;
    }

    public LocalDateTime getFechaFinalizacion() {
        return fechaFinalizacion;
    }

    public void setFechaFinalizacion(LocalDateTime fechaFinalizacion) {
        this.fechaFinalizacion = fechaFinalizacion;
    }
}
