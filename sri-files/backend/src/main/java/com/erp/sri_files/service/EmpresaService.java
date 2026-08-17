package com.erp.sri_files.service;

import com.erp.sri_files.config.AESUtil;
import com.erp.sri_files.domain.auth.EmpresaAuditoria;
import com.erp.sri_files.domain.documento.Empresa;
import com.erp.sri_files.dto.request.EmpresaConfiguracionRequest;
import com.erp.sri_files.dto.request.EmpresaEstadoRequest;
import com.erp.sri_files.dto.request.EmpresaRequest;
import com.erp.sri_files.dto.response.EmpresaAuditoriaListadoItemResponse;
import com.erp.sri_files.dto.response.EmpresaAuditoriaListadoResponse;
import com.erp.sri_files.dto.response.EmpresaAuditoriaResponse;
import com.erp.sri_files.dto.response.EmpresaConfiguracionResponse;
import com.erp.sri_files.dto.response.EmpresaListadoResponse;
import com.erp.sri_files.dto.response.EmpresaResponse;
import com.erp.sri_files.dto.response.UsuarioAutenticadoResponse;
import com.erp.sri_files.exceptions.DocumentoRecepcionException;
import com.erp.sri_files.repositories.auth.EmpresaAuditoriaRepository;
import com.erp.sri_files.repositories.documento.DocumentoElectronicoRepository;
import com.erp.sri_files.repositories.documento.EmpresaRepository;
import com.erp.sri_files.services.KeystoreProbe;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.security.cert.X509Certificate;
import java.util.Base64;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Service
public class EmpresaService {

    private final EmpresaRepository empresaRepository;
    private final DocumentoElectronicoRepository documentoElectronicoRepository;
    private final EmpresaAuditoriaRepository empresaAuditoriaRepository;

    public EmpresaService(
            EmpresaRepository empresaRepository,
            DocumentoElectronicoRepository documentoElectronicoRepository,
            EmpresaAuditoriaRepository empresaAuditoriaRepository
    ) {
        this.empresaRepository = empresaRepository;
        this.documentoElectronicoRepository = documentoElectronicoRepository;
        this.empresaAuditoriaRepository = empresaAuditoriaRepository;
    }

    @Transactional(readOnly = true)
    public EmpresaListadoResponse listar(int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<Empresa> empresas = empresaRepository.findAllByOrderByRazonSocialAsc(pageable);
        return new EmpresaListadoResponse(
                empresas.getContent().stream().map(this::mapear).toList(),
                empresas.getNumber(),
                empresas.getSize(),
                empresas.getTotalElements(),
                empresas.getTotalPages()
        );
    }

    @Transactional(readOnly = true)
    public EmpresaResponse obtener(UUID uuid) {
        return mapear(buscar(uuid));
    }

    @Transactional(readOnly = true)
    public EmpresaConfiguracionResponse obtenerConfiguracion(UUID uuid) {
        return mapearConfiguracion(buscar(uuid));
    }

    @Transactional
    public EmpresaResponse crear(EmpresaRequest request, UsuarioAutenticadoResponse actor) {
        empresaRepository.findByRuc(request.ruc().trim())
                .ifPresent(empresa -> {
                    throw new DocumentoRecepcionException("Ya existe una empresa registrada con el RUC " + request.ruc().trim());
                });

        Empresa empresa = new Empresa();
        empresa.setUuid(UUID.randomUUID());
        aplicarCambios(empresa, request, true);
        empresa.setActivo(true);

        Empresa saved = empresaRepository.save(empresa);
        registrarAuditoria(saved, actor, "EMPRESA_CREADA",
                "Empresa " + saved.getRazonSocial() + " creada con RUC " + saved.getRuc());
        return mapear(saved);
    }

    @Transactional
    public EmpresaResponse actualizar(UUID uuid, EmpresaRequest request, UsuarioAutenticadoResponse actor) {
        Empresa empresa = buscar(uuid);
        String nuevoRuc = request.ruc().trim();
        String rucAnterior = empresa.getRuc();
        String razonSocialAnterior = empresa.getRazonSocial();
        String nombreComercialAnterior = empresa.getNombreComercial();
        String direccionAnterior = empresa.getDireccionMatriz();
        boolean obligadoAnterior = empresa.isObligadoContabilidad();
        String contribuyenteAnterior = empresa.getContribuyenteEspecial();

        if (!empresa.getRuc().equals(nuevoRuc) && documentoElectronicoRepository.existsByEmpresaId(empresa.getId())) {
            throw new DocumentoRecepcionException(
                    "No se puede modificar el RUC de una empresa que ya tiene documentos registrados"
            );
        }

        empresaRepository.findByRuc(nuevoRuc)
                .filter(existente -> !existente.getUuid().equals(empresa.getUuid()))
                .ifPresent(existente -> {
                    throw new DocumentoRecepcionException("Ya existe otra empresa registrada con el RUC " + nuevoRuc);
                });

        aplicarCambios(empresa, request, false);
        Empresa saved = empresaRepository.save(empresa);
        registrarAuditoria(saved, actor, "EMPRESA_ACTUALIZADA",
                construirDescripcionActualizacion(
                        saved,
                        rucAnterior,
                        razonSocialAnterior,
                        nombreComercialAnterior,
                        direccionAnterior,
                        obligadoAnterior,
                        contribuyenteAnterior
                ));
        return mapear(saved);
    }

    @Transactional
    public EmpresaResponse actualizarEstado(UUID uuid, EmpresaEstadoRequest request, UsuarioAutenticadoResponse actor) {
        Empresa empresa = buscar(uuid);
        boolean estadoAnterior = empresa.isActivo();
        empresa.setActivo(request.activo());
        Empresa saved = empresaRepository.save(empresa);
        if (estadoAnterior != saved.isActivo()) {
            registrarAuditoria(saved, actor, "EMPRESA_ESTADO_ACTUALIZADO",
                    "Empresa " + saved.getRazonSocial() + " cambio de estado: "
                            + (estadoAnterior ? "ACTIVA" : "INACTIVA")
                            + " -> "
                            + (saved.isActivo() ? "ACTIVA" : "INACTIVA"));
        }
        return mapear(saved);
    }

    @Transactional
    public EmpresaConfiguracionResponse actualizarConfiguracion(UUID uuid, EmpresaConfiguracionRequest request, UsuarioAutenticadoResponse actor) {
        Empresa empresa = buscar(uuid);
        Short ambienteAnterior = empresa.getSriAmbiente();
        String correoNotificacionesAnterior = empresa.getCorreoNotificaciones();
        String correoRespuestaAnterior = empresa.getCorreoRespuesta();
        String certificadoNombreAnterior = empresa.getCertificadoNombre();
        boolean teniaCertificado = empresa.getCertificadoPkcs12() != null && empresa.getCertificadoPkcs12().length > 0;
        empresa.setSriAmbiente(request.ambienteSri().shortValue());
        empresa.setCorreoNotificaciones(normalizar(request.correoNotificaciones()));
        empresa.setCorreoRespuesta(normalizar(request.correoRespuesta()));

        if (request.limpiarCertificado()) {
            empresa.setCertificadoPkcs12(null);
            empresa.setCertificadoClave(null);
            empresa.setCertificadoNombre(null);
        }

        boolean envioCertificado = request.certificadoBase64() != null && !request.certificadoBase64().isBlank();
        if (envioCertificado) {
            if (request.certificadoClave() == null) {
                throw new DocumentoRecepcionException("La clave del certificado es obligatoria cuando se carga un PKCS12");
            }

            byte[] certificado;
            try {
                certificado = Base64.getDecoder().decode(request.certificadoBase64().trim());
            } catch (IllegalArgumentException ex) {
                throw new DocumentoRecepcionException("El certificado debe estar codificado en Base64 valido");
            }

            try {
                KeystoreProbe.probePkcs12(certificado, request.certificadoClave());
                empresa.setCertificadoPkcs12(certificado);
                empresa.setCertificadoClave(AESUtil.cifrar(request.certificadoClave()));
                empresa.setCertificadoNombre(normalizar(request.certificadoNombre()));
            } catch (DocumentoRecepcionException ex) {
                throw ex;
            } catch (Exception ex) {
                throw new DocumentoRecepcionException("No fue posible validar el certificado PKCS12: " + ex.getMessage());
            }
        } else if (request.certificadoNombre() != null && !request.certificadoNombre().isBlank() && empresa.getCertificadoPkcs12() != null) {
            empresa.setCertificadoNombre(normalizar(request.certificadoNombre()));
        }

        Empresa saved = empresaRepository.save(empresa);
        registrarAuditoria(saved, actor, "EMPRESA_CONFIGURACION_ACTUALIZADA",
                construirDescripcionConfiguracion(
                        saved,
                        ambienteAnterior,
                        correoNotificacionesAnterior,
                        correoRespuestaAnterior,
                        certificadoNombreAnterior,
                        teniaCertificado,
                        request.limpiarCertificado(),
                        envioCertificado
                ));
        return mapearConfiguracion(saved);
    }

    @Transactional(readOnly = true)
    public List<EmpresaAuditoriaResponse> obtenerAuditoria(UUID uuid) {
        Empresa empresa = buscar(uuid);
        return empresaAuditoriaRepository.findTop20ByEmpresaOrderByCreatedAtDesc(empresa).stream()
                .map(item -> new EmpresaAuditoriaResponse(
                        item.getAccion(),
                        item.getDescripcion(),
                        item.getActorUsername(),
                        item.getCreatedAt().toString()
                ))
                .toList();
    }

    @Transactional(readOnly = true)
    public EmpresaAuditoriaListadoResponse listarAuditoriaReciente(int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<EmpresaAuditoria> auditoria = empresaAuditoriaRepository.findAllByOrderByCreatedAtDesc(pageable);

        return new EmpresaAuditoriaListadoResponse(
                auditoria.getContent().stream().map(item -> new EmpresaAuditoriaListadoItemResponse(
                        item.getId(),
                        item.getEmpresa() != null ? item.getEmpresa().getUuid().toString() : null,
                        item.getEmpresa() != null ? item.getEmpresa().getRuc() : null,
                        item.getEmpresa() != null ? item.getEmpresa().getRazonSocial() : null,
                        item.getAccion(),
                        item.getDescripcion(),
                        item.getActorUsername(),
                        item.getCreatedAt().toString()
                )).toList(),
                auditoria.getNumber(),
                auditoria.getSize(),
                auditoria.getTotalElements(),
                auditoria.getTotalPages()
        );
    }

    private void aplicarCambios(Empresa empresa, EmpresaRequest request, boolean incluirRuc) {
        if (incluirRuc || request.ruc() != null) {
            empresa.setRuc(request.ruc().trim());
        }
        empresa.setRazonSocial(request.razonSocial().trim());
        empresa.setNombreComercial(normalizar(request.nombreComercial()));
        empresa.setDireccionMatriz(normalizar(request.direccionMatriz()));
        empresa.setObligadoContabilidad(request.obligadoContabilidad());
        empresa.setContribuyenteEspecial(normalizar(request.contribuyenteEspecial()));
    }

    private String normalizar(String value) {
        if (value == null) {
            return null;
        }

        String limpio = value.trim();
        return limpio.isEmpty() ? null : limpio;
    }

    private Empresa buscar(UUID uuid) {
        return empresaRepository.findByUuid(uuid)
                .orElseThrow(() -> new DocumentoRecepcionException("No existe empresa con uuid " + uuid));
    }

    private void registrarAuditoria(Empresa empresa, UsuarioAutenticadoResponse actor, String accion, String descripcion) {
        EmpresaAuditoria auditoria = new EmpresaAuditoria();
        auditoria.setEmpresa(empresa);
        auditoria.setActorUuid(actor != null && actor.id() != null ? UUID.fromString(actor.id()) : null);
        auditoria.setActorUsername(actor != null ? actor.nombre() : "sistema");
        auditoria.setAccion(accion);
        auditoria.setDescripcion(descripcion);
        auditoria.setCreatedAt(LocalDateTime.now());
        empresaAuditoriaRepository.save(auditoria);
    }

    private String construirDescripcionActualizacion(
            Empresa empresa,
            String rucAnterior,
            String razonSocialAnterior,
            String nombreComercialAnterior,
            String direccionAnterior,
            boolean obligadoAnterior,
            String contribuyenteAnterior
    ) {
        StringBuilder builder = new StringBuilder("Empresa ").append(empresa.getRazonSocial()).append(" actualizada");
        appendCambio(builder, "ruc", rucAnterior, empresa.getRuc());
        appendCambio(builder, "razon social", razonSocialAnterior, empresa.getRazonSocial());
        appendCambio(builder, "nombre comercial", nombreComercialAnterior, empresa.getNombreComercial());
        appendCambio(builder, "direccion matriz", direccionAnterior, empresa.getDireccionMatriz());
        if (obligadoAnterior != empresa.isObligadoContabilidad()) {
            builder.append("; obligado contabilidad: ").append(obligadoAnterior).append(" -> ").append(empresa.isObligadoContabilidad());
        }
        appendCambio(builder, "contribuyente especial", contribuyenteAnterior, empresa.getContribuyenteEspecial());
        return builder.toString();
    }

    private String construirDescripcionConfiguracion(
            Empresa empresa,
            Short ambienteAnterior,
            String correoNotificacionesAnterior,
            String correoRespuestaAnterior,
            String certificadoNombreAnterior,
            boolean teniaCertificado,
            boolean limpiarCertificado,
            boolean envioCertificado
    ) {
        StringBuilder builder = new StringBuilder("Configuracion sensible actualizada para ").append(empresa.getRazonSocial());
        if (!Objects.equals(ambienteAnterior, empresa.getSriAmbiente())) {
            builder.append("; ambiente SRI: ").append(ambienteAnterior).append(" -> ").append(empresa.getSriAmbiente());
        }
        appendCambio(builder, "correo notificaciones", correoNotificacionesAnterior, empresa.getCorreoNotificaciones());
        appendCambio(builder, "correo respuesta", correoRespuestaAnterior, empresa.getCorreoRespuesta());
        if (limpiarCertificado && teniaCertificado) {
            builder.append("; certificado eliminado");
        } else if (envioCertificado) {
            builder.append("; certificado cargado/actualizado");
        } else if (!Objects.equals(normalizar(certificadoNombreAnterior), normalizar(empresa.getCertificadoNombre()))) {
            builder.append("; nombre certificado: ")
                    .append(valorAuditoria(certificadoNombreAnterior))
                    .append(" -> ")
                    .append(valorAuditoria(empresa.getCertificadoNombre()));
        }
        return builder.toString();
    }

    private void appendCambio(StringBuilder builder, String campo, String anterior, String actual) {
        if (!Objects.equals(normalizar(anterior), normalizar(actual))) {
            builder.append("; ").append(campo).append(": ")
                    .append(valorAuditoria(anterior))
                    .append(" -> ")
                    .append(valorAuditoria(actual));
        }
    }

    private String valorAuditoria(String value) {
        String normalizado = normalizar(value);
        return normalizado != null ? normalizado : "null";
    }

    private EmpresaResponse mapear(Empresa empresa) {
        return new EmpresaResponse(
                empresa.getUuid().toString(),
                empresa.getRuc(),
                empresa.getRazonSocial(),
                empresa.getNombreComercial(),
                empresa.getDireccionMatriz(),
                empresa.isObligadoContabilidad(),
                empresa.getContribuyenteEspecial(),
                empresa.getSriAmbiente(),
                empresa.getCorreoNotificaciones(),
                empresa.getCertificadoPkcs12() != null && empresa.getCertificadoPkcs12().length > 0,
                empresa.isActivo()
        );
    }

    private EmpresaConfiguracionResponse mapearConfiguracion(Empresa empresa) {
        String alias = null;
        String titular = null;
        String vigenciaDesde = null;
        String vigenciaHasta = null;

        if (empresa.getCertificadoPkcs12() != null && empresa.getCertificadoPkcs12().length > 0 && empresa.getCertificadoClave() != null) {
            try {
                String clave = AESUtil.descifrar(empresa.getCertificadoClave());
                KeystoreProbe.Result probe = KeystoreProbe.probePkcs12(empresa.getCertificadoPkcs12(), clave);
                alias = probe.alias();
                X509Certificate cert = probe.cert();
                if (cert != null) {
                    titular = cert.getSubjectX500Principal().getName();
                    vigenciaDesde = cert.getNotBefore().toInstant().toString();
                    vigenciaHasta = cert.getNotAfter().toInstant().toString();
                }
            } catch (Exception ignored) {
                alias = "NO_VALIDO";
            }
        }

        return new EmpresaConfiguracionResponse(
                empresa.getUuid().toString(),
                empresa.getSriAmbiente(),
                empresa.getCorreoNotificaciones(),
                empresa.getCorreoRespuesta(),
                empresa.getCertificadoPkcs12() != null && empresa.getCertificadoPkcs12().length > 0,
                empresa.getCertificadoNombre(),
                alias,
                titular,
                vigenciaDesde,
                vigenciaHasta
        );
    }
}
