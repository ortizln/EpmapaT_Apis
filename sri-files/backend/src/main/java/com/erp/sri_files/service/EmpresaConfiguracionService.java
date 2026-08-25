package com.erp.sri_files.service;

import com.erp.sri_files.config.AESUtil;
import com.erp.sri_files.domain.documento.Empresa;
import com.erp.sri_files.dto.request.CertificadoEstadoRequest;
import com.erp.sri_files.dto.request.CorreoConfiguracionRequest;
import com.erp.sri_files.dto.request.SriConfiguracionRequest;
import com.erp.sri_files.dto.response.CertificadoResponse;
import com.erp.sri_files.dto.response.CorreoConfiguracionResponse;
import com.erp.sri_files.dto.response.SriConfiguracionResponse;
import com.erp.sri_files.dto.response.UsuarioAutenticadoResponse;
import com.erp.sri_files.dto.response.VerificacionCertificadoResponse;
import com.erp.sri_files.exceptions.DocumentoRecepcionException;
import com.erp.sri_files.repositories.documento.EmpresaRepository;
import com.erp.sri_files.services.KeystoreProbe;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.security.cert.X509Certificate;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

@Service
public class EmpresaConfiguracionService {

    private final EmpresaRepository empresaRepository;
    private final EmpresaService empresaService;

    public EmpresaConfiguracionService(EmpresaRepository empresaRepository, EmpresaService empresaService) {
        this.empresaRepository = empresaRepository;
        this.empresaService = empresaService;
    }

    @Transactional(readOnly = true)
    public List<CertificadoResponse> listarCertificados(UUID empresaId) {
        Empresa empresa = buscar(empresaId);
        if (empresa.getCertificadoPkcs12() == null || empresa.getCertificadoPkcs12().length == 0) {
            return List.of();
        }
        return List.of(mapearCertificado(empresa));
    }

    @Transactional
    public CertificadoResponse cargarCertificado(UUID empresaId, MultipartFile file, String nombre, String clave, UsuarioAutenticadoResponse actor) {
        Empresa empresa = buscar(empresaId);
        if (file == null || file.isEmpty()) {
            throw new DocumentoRecepcionException("El archivo del certificado es obligatorio");
        }
        if (clave == null || clave.isBlank()) {
            throw new DocumentoRecepcionException("La clave del certificado es obligatoria");
        }

        byte[] contenido;
        try {
            contenido = file.getBytes();
        } catch (IOException ex) {
            throw new DocumentoRecepcionException("No fue posible leer el archivo del certificado");
        }

        validarCertificado(contenido, clave);
        empresa.setCertificadoPkcs12(contenido);
        try {
            empresa.setCertificadoClave(AESUtil.cifrar(clave));
        } catch (Exception ex) {
            throw new DocumentoRecepcionException("No fue posible proteger la clave del certificado");
        }
        empresa.setCertificadoNombre(normalizar(nombre != null ? nombre : file.getOriginalFilename()));
        empresa.setCertificadoActivo(true);
        empresaRepository.save(empresa);
        empresaService.registrarAuditoriaConfiguracion(empresa, actor, "CERTIFICADO_CARGADO",
                "Certificado cargado/actualizado para " + empresa.getRazonSocial());
        return mapearCertificado(empresa);
    }

    @Transactional(readOnly = true)
    public VerificacionCertificadoResponse verificarCertificado(UUID uuid) {
        Empresa empresa = buscar(uuid);
        CertificadoMeta meta = extraerMetaCertificado(empresa);
        return new VerificacionCertificadoResponse(
                meta.valido(),
                meta.fechaEmision(),
                meta.fechaExpiracion(),
                meta.diasRestantes()
        );
    }

    @Transactional
    public CertificadoResponse actualizarEstadoCertificado(UUID uuid, CertificadoEstadoRequest request, UsuarioAutenticadoResponse actor) {
        Empresa empresa = buscar(uuid);
        if (empresa.getCertificadoPkcs12() == null || empresa.getCertificadoPkcs12().length == 0) {
            throw new DocumentoRecepcionException("La empresa no tiene certificado configurado");
        }
        empresa.setCertificadoActivo(Boolean.TRUE.equals(request.activo()));
        empresaRepository.save(empresa);
        empresaService.registrarAuditoriaConfiguracion(empresa, actor, "CERTIFICADO_ESTADO_ACTUALIZADO",
                "Certificado " + (empresa.isCertificadoActivo() ? "activado" : "desactivado") + " para " + empresa.getRazonSocial());
        return mapearCertificado(empresa);
    }

    @Transactional(readOnly = true)
    public SriConfiguracionResponse obtenerConfiguracionSri(UUID empresaId) {
        Empresa empresa = buscar(empresaId);
        return new SriConfiguracionResponse(
                empresa.getUuid().toString(),
                empresa.getSriAmbiente(),
                empresa.getSriTimeoutConexionMs(),
                empresa.getSriTimeoutRespuestaMs(),
                empresa.getSriMaxReintentos(),
                empresa.isActivo()
        );
    }

    @Transactional
    public SriConfiguracionResponse actualizarConfiguracionSri(UUID empresaId, SriConfiguracionRequest request, UsuarioAutenticadoResponse actor) {
        Empresa empresa = buscar(empresaId);
        empresa.setSriAmbiente(request.ambiente().shortValue());
        empresa.setSriTimeoutConexionMs(request.timeoutConexionMs());
        empresa.setSriTimeoutRespuestaMs(request.timeoutRespuestaMs());
        empresa.setSriMaxReintentos(request.maxReintentos());
        empresa.setActivo(Boolean.TRUE.equals(request.activo()));
        empresaRepository.save(empresa);
        empresaService.registrarAuditoriaConfiguracion(empresa, actor, "CONFIGURACION_SRI_ACTUALIZADA",
                "Configuracion SRI actualizada para " + empresa.getRazonSocial());
        return obtenerConfiguracionSri(empresaId);
    }

    @Transactional(readOnly = true)
    public CorreoConfiguracionResponse obtenerConfiguracionCorreo(UUID empresaId) {
        Empresa empresa = buscar(empresaId);
        return new CorreoConfiguracionResponse(
                empresa.getUuid().toString(),
                empresa.getCorreoNotificaciones(),
                empresa.getCorreoNombreRemitente(),
                empresa.isCorreoEnviarXml(),
                empresa.isCorreoEnviarRide(),
                empresa.getCorreoPlantillaAsunto()
        );
    }

    @Transactional
    public CorreoConfiguracionResponse actualizarConfiguracionCorreo(UUID empresaId, CorreoConfiguracionRequest request, UsuarioAutenticadoResponse actor) {
        Empresa empresa = buscar(empresaId);
        empresa.setCorreoNotificaciones(normalizar(request.remitente()));
        empresa.setCorreoNombreRemitente(normalizar(request.nombreRemitente()));
        empresa.setCorreoEnviarXml(request.enviarXml());
        empresa.setCorreoEnviarRide(request.enviarRide());
        empresa.setCorreoPlantillaAsunto(normalizar(request.plantillaAsunto()));
        empresaRepository.save(empresa);
        empresaService.registrarAuditoriaConfiguracion(empresa, actor, "CONFIGURACION_CORREO_ACTUALIZADA",
                "Configuracion de correo actualizada para " + empresa.getRazonSocial());
        return obtenerConfiguracionCorreo(empresaId);
    }

    private Empresa buscar(UUID uuid) {
        return empresaRepository.findByUuid(uuid)
                .orElseThrow(() -> new DocumentoRecepcionException("No existe empresa con uuid " + uuid));
    }

    private CertificadoResponse mapearCertificado(Empresa empresa) {
        CertificadoMeta meta = extraerMetaCertificado(empresa);
        return new CertificadoResponse(
                empresa.getUuid().toString(),
                empresa.getUuid().toString(),
                empresa.getCertificadoNombre(),
                empresa.isCertificadoActivo(),
                meta.valido(),
                meta.alias(),
                meta.titular(),
                meta.fechaEmision(),
                meta.fechaExpiracion(),
                meta.diasRestantes()
        );
    }

    private CertificadoMeta extraerMetaCertificado(Empresa empresa) {
        if (empresa.getCertificadoPkcs12() == null || empresa.getCertificadoPkcs12().length == 0 || empresa.getCertificadoClave() == null) {
            return new CertificadoMeta(false, null, null, null, null, null);
        }
        try {
            String clave = AESUtil.descifrar(empresa.getCertificadoClave());
            KeystoreProbe.Result probe = KeystoreProbe.probePkcs12(empresa.getCertificadoPkcs12(), clave);
            X509Certificate cert = probe.cert();
            Instant emision = cert.getNotBefore().toInstant();
            Instant expiracion = cert.getNotAfter().toInstant();
            long diasRestantes = Instant.now().until(expiracion, ChronoUnit.DAYS);
            return new CertificadoMeta(
                    true,
                    probe.alias(),
                    cert.getSubjectX500Principal().getName(),
                    emision.toString(),
                    expiracion.toString(),
                    diasRestantes
            );
        } catch (Exception ex) {
            return new CertificadoMeta(false, "NO_VALIDO", null, null, null, null);
        }
    }

    private void validarCertificado(byte[] certificado, String clave) {
        try {
            KeystoreProbe.probePkcs12(certificado, clave);
        } catch (Exception ex) {
            throw new DocumentoRecepcionException("No fue posible validar el certificado PKCS12: " + ex.getMessage());
        }
    }

    private String normalizar(String value) {
        if (value == null) {
            return null;
        }
        String limpio = value.trim();
        return limpio.isEmpty() ? null : limpio;
    }

    private record CertificadoMeta(
            boolean valido,
            String alias,
            String titular,
            String fechaEmision,
            String fechaExpiracion,
            Long diasRestantes
    ) {
    }
}
