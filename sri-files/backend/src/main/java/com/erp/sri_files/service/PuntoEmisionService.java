package com.erp.sri_files.service;

import com.erp.sri_files.domain.documento.Establecimiento;
import com.erp.sri_files.domain.documento.PuntoEmision;
import com.erp.sri_files.dto.request.PuntoEmisionEstadoRequest;
import com.erp.sri_files.dto.request.PuntoEmisionRequest;
import com.erp.sri_files.dto.response.PuntoEmisionResponse;
import com.erp.sri_files.exceptions.DocumentoRecepcionException;
import com.erp.sri_files.repositories.documento.EstablecimientoRepository;
import com.erp.sri_files.repositories.documento.PuntoEmisionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class PuntoEmisionService {

    private final EstablecimientoRepository establecimientoRepository;
    private final PuntoEmisionRepository puntoEmisionRepository;

    public PuntoEmisionService(
            EstablecimientoRepository establecimientoRepository,
            PuntoEmisionRepository puntoEmisionRepository
    ) {
        this.establecimientoRepository = establecimientoRepository;
        this.puntoEmisionRepository = puntoEmisionRepository;
    }

    @Transactional(readOnly = true)
    public List<PuntoEmisionResponse> listarPorEstablecimiento(UUID establecimientoUuid) {
        Establecimiento establecimiento = buscarEstablecimiento(establecimientoUuid);
        return puntoEmisionRepository.findByEstablecimientoOrderByCodigoAsc(establecimiento).stream()
                .map(this::mapear)
                .toList();
    }

    @Transactional(readOnly = true)
    public PuntoEmisionResponse obtener(UUID uuid) {
        return mapear(buscarPuntoEmision(uuid));
    }

    @Transactional
    public PuntoEmisionResponse crear(UUID establecimientoUuid, PuntoEmisionRequest request) {
        Establecimiento establecimiento = buscarEstablecimiento(establecimientoUuid);
        String codigo = request.codigo().trim();

        puntoEmisionRepository.findByEstablecimientoAndCodigo(establecimiento, codigo)
                .ifPresent(existente -> {
                    throw new DocumentoRecepcionException(
                            "Ya existe un punto de emision con codigo " + codigo + " para el establecimiento seleccionado"
                    );
                });

        PuntoEmision puntoEmision = new PuntoEmision();
        puntoEmision.setUuid(UUID.randomUUID());
        puntoEmision.setEstablecimiento(establecimiento);
        aplicarCambios(puntoEmision, request);
        puntoEmision.setActivo(true);

        return mapear(puntoEmisionRepository.save(puntoEmision));
    }

    @Transactional
    public PuntoEmisionResponse actualizar(UUID uuid, PuntoEmisionRequest request) {
        PuntoEmision puntoEmision = buscarPuntoEmision(uuid);
        String codigo = request.codigo().trim();

        puntoEmisionRepository.findByEstablecimientoAndCodigo(puntoEmision.getEstablecimiento(), codigo)
                .filter(existente -> !existente.getUuid().equals(puntoEmision.getUuid()))
                .ifPresent(existente -> {
                    throw new DocumentoRecepcionException(
                            "Ya existe otro punto de emision con codigo " + codigo + " para el establecimiento seleccionado"
                    );
                });

        aplicarCambios(puntoEmision, request);
        return mapear(puntoEmisionRepository.save(puntoEmision));
    }

    @Transactional
    public PuntoEmisionResponse actualizarEstado(UUID uuid, PuntoEmisionEstadoRequest request) {
        PuntoEmision puntoEmision = buscarPuntoEmision(uuid);
        puntoEmision.setActivo(request.activo());
        return mapear(puntoEmisionRepository.save(puntoEmision));
    }

    private void aplicarCambios(PuntoEmision puntoEmision, PuntoEmisionRequest request) {
        puntoEmision.setCodigo(request.codigo().trim());
        puntoEmision.setNombre(normalizar(request.nombre()));
    }

    private Establecimiento buscarEstablecimiento(UUID uuid) {
        return establecimientoRepository.findByUuid(uuid)
                .orElseThrow(() -> new DocumentoRecepcionException("No existe establecimiento con uuid " + uuid));
    }

    private PuntoEmision buscarPuntoEmision(UUID uuid) {
        return puntoEmisionRepository.findByUuid(uuid)
                .orElseThrow(() -> new DocumentoRecepcionException("No existe punto de emision con uuid " + uuid));
    }

    private String normalizar(String value) {
        if (value == null) {
            return null;
        }

        String limpio = value.trim();
        return limpio.isEmpty() ? null : limpio;
    }

    private PuntoEmisionResponse mapear(PuntoEmision puntoEmision) {
        return new PuntoEmisionResponse(
                puntoEmision.getUuid().toString(),
                puntoEmision.getEstablecimiento().getUuid().toString(),
                puntoEmision.getEstablecimiento().getCodigo(),
                puntoEmision.getEstablecimiento().getEmpresa().getUuid().toString(),
                puntoEmision.getEstablecimiento().getEmpresa().getRazonSocial(),
                puntoEmision.getCodigo(),
                puntoEmision.getNombre(),
                puntoEmision.isActivo()
        );
    }
}
