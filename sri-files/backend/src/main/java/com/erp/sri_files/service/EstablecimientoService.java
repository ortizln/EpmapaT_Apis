package com.erp.sri_files.service;

import com.erp.sri_files.domain.documento.Empresa;
import com.erp.sri_files.domain.documento.Establecimiento;
import com.erp.sri_files.dto.request.EstablecimientoEstadoRequest;
import com.erp.sri_files.dto.request.EstablecimientoRequest;
import com.erp.sri_files.dto.response.EstablecimientoResponse;
import com.erp.sri_files.exceptions.DocumentoRecepcionException;
import com.erp.sri_files.repositories.documento.EmpresaRepository;
import com.erp.sri_files.repositories.documento.EstablecimientoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class EstablecimientoService {

    private final EmpresaRepository empresaRepository;
    private final EstablecimientoRepository establecimientoRepository;

    public EstablecimientoService(
            EmpresaRepository empresaRepository,
            EstablecimientoRepository establecimientoRepository
    ) {
        this.empresaRepository = empresaRepository;
        this.establecimientoRepository = establecimientoRepository;
    }

    @Transactional(readOnly = true)
    public List<EstablecimientoResponse> listarPorEmpresa(UUID empresaUuid) {
        Empresa empresa = buscarEmpresa(empresaUuid);
        return establecimientoRepository.findByEmpresaOrderByCodigoAsc(empresa).stream()
                .map(this::mapear)
                .toList();
    }

    @Transactional(readOnly = true)
    public EstablecimientoResponse obtener(UUID uuid) {
        return mapear(buscarEstablecimiento(uuid));
    }

    @Transactional
    public EstablecimientoResponse crear(UUID empresaUuid, EstablecimientoRequest request) {
        Empresa empresa = buscarEmpresa(empresaUuid);
        String codigo = request.codigo().trim();

        establecimientoRepository.findByEmpresaAndCodigo(empresa, codigo)
                .ifPresent(existente -> {
                    throw new DocumentoRecepcionException(
                            "Ya existe un establecimiento con codigo " + codigo + " para la empresa seleccionada"
                    );
                });

        Establecimiento establecimiento = new Establecimiento();
        establecimiento.setUuid(UUID.randomUUID());
        establecimiento.setEmpresa(empresa);
        aplicarCambios(establecimiento, request);
        establecimiento.setActivo(true);

        return mapear(establecimientoRepository.save(establecimiento));
    }

    @Transactional
    public EstablecimientoResponse actualizar(UUID uuid, EstablecimientoRequest request) {
        Establecimiento establecimiento = buscarEstablecimiento(uuid);
        String codigo = request.codigo().trim();

        establecimientoRepository.findByEmpresaAndCodigo(establecimiento.getEmpresa(), codigo)
                .filter(existente -> !existente.getUuid().equals(establecimiento.getUuid()))
                .ifPresent(existente -> {
                    throw new DocumentoRecepcionException(
                            "Ya existe otro establecimiento con codigo " + codigo + " para la empresa seleccionada"
                    );
                });

        aplicarCambios(establecimiento, request);
        return mapear(establecimientoRepository.save(establecimiento));
    }

    @Transactional
    public EstablecimientoResponse actualizarEstado(UUID uuid, EstablecimientoEstadoRequest request) {
        Establecimiento establecimiento = buscarEstablecimiento(uuid);
        establecimiento.setActivo(request.activo());
        return mapear(establecimientoRepository.save(establecimiento));
    }

    private void aplicarCambios(Establecimiento establecimiento, EstablecimientoRequest request) {
        establecimiento.setCodigo(request.codigo().trim());
        establecimiento.setNombre(normalizar(request.nombre()));
        establecimiento.setDireccion(normalizar(request.direccion()));
    }

    private Empresa buscarEmpresa(UUID uuid) {
        return empresaRepository.findByUuid(uuid)
                .orElseThrow(() -> new DocumentoRecepcionException("No existe empresa con uuid " + uuid));
    }

    private Establecimiento buscarEstablecimiento(UUID uuid) {
        return establecimientoRepository.findByUuid(uuid)
                .orElseThrow(() -> new DocumentoRecepcionException("No existe establecimiento con uuid " + uuid));
    }

    private String normalizar(String value) {
        if (value == null) {
            return null;
        }

        String limpio = value.trim();
        return limpio.isEmpty() ? null : limpio;
    }

    private EstablecimientoResponse mapear(Establecimiento establecimiento) {
        return new EstablecimientoResponse(
                establecimiento.getUuid().toString(),
                establecimiento.getEmpresa().getUuid().toString(),
                establecimiento.getEmpresa().getRazonSocial(),
                establecimiento.getCodigo(),
                establecimiento.getNombre(),
                establecimiento.getDireccion(),
                establecimiento.isActivo()
        );
    }
}
