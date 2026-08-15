package com.erp.sri_files.service;

import com.erp.sri_files.config.AESUtil;
import com.erp.sri_files.domain.documento.Empresa;
import com.erp.sri_files.dto.request.EmpresaConfiguracionRequest;
import com.erp.sri_files.dto.request.EmpresaEstadoRequest;
import com.erp.sri_files.dto.request.EmpresaRequest;
import com.erp.sri_files.dto.response.EmpresaConfiguracionResponse;
import com.erp.sri_files.dto.response.EmpresaListadoResponse;
import com.erp.sri_files.dto.response.EmpresaResponse;
import com.erp.sri_files.exceptions.DocumentoRecepcionException;
import com.erp.sri_files.repositories.documento.DocumentoElectronicoRepository;
import com.erp.sri_files.repositories.documento.EmpresaRepository;
import com.erp.sri_files.services.KeystoreProbe;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.cert.X509Certificate;
import java.util.Base64;
import java.util.List;
import java.util.UUID;

@Service
public class EmpresaService {

    private final EmpresaRepository empresaRepository;
    private final DocumentoElectronicoRepository documentoElectronicoRepository;

    public EmpresaService(
            EmpresaRepository empresaRepository,
            DocumentoElectronicoRepository documentoElectronicoRepository
    ) {
        this.empresaRepository = empresaRepository;
        this.documentoElectronicoRepository = documentoElectronicoRepository;
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
    public EmpresaResponse crear(EmpresaRequest request) {
        empresaRepository.findByRuc(request.ruc().trim())
                .ifPresent(empresa -> {
                    throw new DocumentoRecepcionException("Ya existe una empresa registrada con el RUC " + request.ruc().trim());
                });

        Empresa empresa = new Empresa();
        empresa.setUuid(UUID.randomUUID());
        aplicarCambios(empresa, request, true);
        empresa.setActivo(true);

        return mapear(empresaRepository.save(empresa));
    }

    @Transactional
    public EmpresaResponse actualizar(UUID uuid, EmpresaRequest request) {
        Empresa empresa = buscar(uuid);
        String nuevoRuc = request.ruc().trim();

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
        return mapear(empresaRepository.save(empresa));
    }

    @Transactional
    public EmpresaResponse actualizarEstado(UUID uuid, EmpresaEstadoRequest request) {
        Empresa empresa = buscar(uuid);
        empresa.setActivo(request.activo());
        return mapear(empresaRepository.save(empresa));
    }

    @Transactional
    public EmpresaConfiguracionResponse actualizarConfiguracion(UUID uuid, EmpresaConfiguracionRequest request) {
        Empresa empresa = buscar(uuid);
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

        return mapearConfiguracion(empresaRepository.save(empresa));
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
