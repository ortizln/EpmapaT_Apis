package com.erp.sri_files.domain.documento;

import com.erp.sri_files.domain.common.AuditableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.util.UUID;

@Entity
@Table(name = "empresa")
public class Empresa extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private UUID uuid;

    @Column(nullable = false, unique = true, length = 13)
    private String ruc;

    @Column(name = "razon_social", nullable = false, length = 300)
    private String razonSocial;

    @Column(name = "nombre_comercial", length = 300)
    private String nombreComercial;

    @Column(name = "direccion_matriz", length = 500)
    private String direccionMatriz;

    @Column(name = "obligado_contabilidad", nullable = false)
    private boolean obligadoContabilidad;

    @Column(name = "contribuyente_especial", length = 50)
    private String contribuyenteEspecial;

    @Column(name = "sri_ambiente", nullable = false)
    private short sriAmbiente = 1;

    @Column(name = "correo_notificaciones", length = 320)
    private String correoNotificaciones;

    @Column(name = "correo_respuesta", length = 320)
    private String correoRespuesta;

    @Column(name = "correo_nombre_remitente", length = 255)
    private String correoNombreRemitente;

    @Column(name = "correo_enviar_xml", nullable = false)
    private boolean correoEnviarXml = true;

    @Column(name = "correo_enviar_ride", nullable = false)
    private boolean correoEnviarRide = true;

    @Column(name = "correo_plantilla_asunto", length = 255)
    private String correoPlantillaAsunto;

    @Column(name = "certificado_nombre", length = 255)
    private String certificadoNombre;

    @Column(name = "certificado_pkcs12")
    private byte[] certificadoPkcs12;

    @Column(name = "certificado_clave", length = 500)
    private String certificadoClave;

    @Column(name = "certificado_activo", nullable = false)
    private boolean certificadoActivo = true;

    @Column(name = "sri_timeout_conexion_ms", nullable = false)
    private int sriTimeoutConexionMs = 10000;

    @Column(name = "sri_timeout_respuesta_ms", nullable = false)
    private int sriTimeoutRespuestaMs = 30000;

    @Column(name = "sri_max_reintentos", nullable = false)
    private int sriMaxReintentos = 5;

    @Column(nullable = false)
    private boolean activo = true;

    public Long getId() {
        return id;
    }

    public UUID getUuid() {
        return uuid;
    }

    public void setUuid(UUID uuid) {
        this.uuid = uuid;
    }

    public String getRuc() {
        return ruc;
    }

    public void setRuc(String ruc) {
        this.ruc = ruc;
    }

    public String getRazonSocial() {
        return razonSocial;
    }

    public void setRazonSocial(String razonSocial) {
        this.razonSocial = razonSocial;
    }

    public String getNombreComercial() {
        return nombreComercial;
    }

    public void setNombreComercial(String nombreComercial) {
        this.nombreComercial = nombreComercial;
    }

    public String getDireccionMatriz() {
        return direccionMatriz;
    }

    public void setDireccionMatriz(String direccionMatriz) {
        this.direccionMatriz = direccionMatriz;
    }

    public boolean isObligadoContabilidad() {
        return obligadoContabilidad;
    }

    public void setObligadoContabilidad(boolean obligadoContabilidad) {
        this.obligadoContabilidad = obligadoContabilidad;
    }

    public String getContribuyenteEspecial() {
        return contribuyenteEspecial;
    }

    public void setContribuyenteEspecial(String contribuyenteEspecial) {
        this.contribuyenteEspecial = contribuyenteEspecial;
    }

    public short getSriAmbiente() {
        return sriAmbiente;
    }

    public void setSriAmbiente(short sriAmbiente) {
        this.sriAmbiente = sriAmbiente;
    }

    public String getCorreoNotificaciones() {
        return correoNotificaciones;
    }

    public void setCorreoNotificaciones(String correoNotificaciones) {
        this.correoNotificaciones = correoNotificaciones;
    }

    public String getCorreoRespuesta() {
        return correoRespuesta;
    }

    public void setCorreoRespuesta(String correoRespuesta) {
        this.correoRespuesta = correoRespuesta;
    }

    public String getCorreoNombreRemitente() {
        return correoNombreRemitente;
    }

    public void setCorreoNombreRemitente(String correoNombreRemitente) {
        this.correoNombreRemitente = correoNombreRemitente;
    }

    public boolean isCorreoEnviarXml() {
        return correoEnviarXml;
    }

    public void setCorreoEnviarXml(boolean correoEnviarXml) {
        this.correoEnviarXml = correoEnviarXml;
    }

    public boolean isCorreoEnviarRide() {
        return correoEnviarRide;
    }

    public void setCorreoEnviarRide(boolean correoEnviarRide) {
        this.correoEnviarRide = correoEnviarRide;
    }

    public String getCorreoPlantillaAsunto() {
        return correoPlantillaAsunto;
    }

    public void setCorreoPlantillaAsunto(String correoPlantillaAsunto) {
        this.correoPlantillaAsunto = correoPlantillaAsunto;
    }

    public String getCertificadoNombre() {
        return certificadoNombre;
    }

    public void setCertificadoNombre(String certificadoNombre) {
        this.certificadoNombre = certificadoNombre;
    }

    public byte[] getCertificadoPkcs12() {
        return certificadoPkcs12;
    }

    public void setCertificadoPkcs12(byte[] certificadoPkcs12) {
        this.certificadoPkcs12 = certificadoPkcs12;
    }

    public String getCertificadoClave() {
        return certificadoClave;
    }

    public void setCertificadoClave(String certificadoClave) {
        this.certificadoClave = certificadoClave;
    }

    public boolean isCertificadoActivo() {
        return certificadoActivo;
    }

    public void setCertificadoActivo(boolean certificadoActivo) {
        this.certificadoActivo = certificadoActivo;
    }

    public int getSriTimeoutConexionMs() {
        return sriTimeoutConexionMs;
    }

    public void setSriTimeoutConexionMs(int sriTimeoutConexionMs) {
        this.sriTimeoutConexionMs = sriTimeoutConexionMs;
    }

    public int getSriTimeoutRespuestaMs() {
        return sriTimeoutRespuestaMs;
    }

    public void setSriTimeoutRespuestaMs(int sriTimeoutRespuestaMs) {
        this.sriTimeoutRespuestaMs = sriTimeoutRespuestaMs;
    }

    public int getSriMaxReintentos() {
        return sriMaxReintentos;
    }

    public void setSriMaxReintentos(int sriMaxReintentos) {
        this.sriMaxReintentos = sriMaxReintentos;
    }

    public boolean isActivo() {
        return activo;
    }

    public void setActivo(boolean activo) {
        this.activo = activo;
    }
}
