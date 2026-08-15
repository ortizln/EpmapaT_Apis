package com.erp.sri_files.service;

import com.erp.sri_files.domain.documento.PuntoEmision;
import com.erp.sri_files.domain.documento.Secuencial;
import com.erp.sri_files.domain.documento.TipoDocumento;
import com.erp.sri_files.dto.request.SecuencialRequest;
import com.erp.sri_files.dto.response.SecuencialResponse;
import com.erp.sri_files.exceptions.DocumentoRecepcionException;
import com.erp.sri_files.repositories.documento.PuntoEmisionRepository;
import com.erp.sri_files.repositories.documento.SecuencialRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class SecuencialService {

    private final PuntoEmisionRepository puntoEmisionRepository;
    private final SecuencialRepository secuencialRepository;

    public SecuencialService(
            PuntoEmisionRepository puntoEmisionRepository,
            SecuencialRepository secuencialRepository
    ) {
        this.puntoEmisionRepository = puntoEmisionRepository;
        this.secuencialRepository = secuencialRepository;
    }

    @Transactional(readOnly = true)
    public List<SecuencialResponse> listarPorPuntoEmision(UUID puntoEmisionUuid) {
        PuntoEmision puntoEmision = buscarPuntoEmision(puntoEmisionUuid);
        Map<TipoDocumento, Secuencial> existentes = new EnumMap<>(TipoDocumento.class);
        secuencialRepository.findByPuntoEmisionOrderByTipoDocumentoAsc(puntoEmision)
                .forEach(item -> existentes.put(item.getTipoDocumento(), item));

        return List.of(TipoDocumento.values()).stream()
                .map(tipo -> existentes.containsKey(tipo) ? mapear(existentes.get(tipo)) : mapearVacio(puntoEmision, tipo))
                .toList();
    }

    @Transactional
    public SecuencialResponse actualizar(UUID puntoEmisionUuid, String tipoDocumento, SecuencialRequest request) {
        PuntoEmision puntoEmision = buscarPuntoEmision(puntoEmisionUuid);
        TipoDocumento tipo = resolverTipo(tipoDocumento);

        Secuencial secuencial = secuencialRepository.findByPuntoEmisionAndTipoDocumento(puntoEmision, tipo)
                .orElseGet(() -> crearNuevo(puntoEmision, tipo));

        secuencial.setValorActual(request.valorActual());
        secuencial.setActivo(request.activo());
        secuencial.setUpdatedAt(LocalDateTime.now());

        return mapear(secuencialRepository.save(secuencial));
    }

    private PuntoEmision buscarPuntoEmision(UUID uuid) {
        return puntoEmisionRepository.findByUuid(uuid)
                .orElseThrow(() -> new DocumentoRecepcionException("No existe punto de emision con uuid " + uuid));
    }

    private TipoDocumento resolverTipo(String tipoDocumento) {
        try {
            return TipoDocumento.valueOf(tipoDocumento.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            throw new DocumentoRecepcionException("Tipo de documento no soportado para secuencial: " + tipoDocumento);
        }
    }

    private Secuencial crearNuevo(PuntoEmision puntoEmision, TipoDocumento tipoDocumento) {
        Secuencial secuencial = new Secuencial();
        secuencial.setPuntoEmision(puntoEmision);
        secuencial.setTipoDocumento(tipoDocumento);
        secuencial.setValorActual(0L);
        secuencial.setActivo(true);
        secuencial.setUpdatedAt(LocalDateTime.now());
        return secuencial;
    }

    private SecuencialResponse mapear(Secuencial secuencial) {
        return new SecuencialResponse(
                secuencial.getPuntoEmision().getUuid().toString(),
                secuencial.getTipoDocumento().name(),
                secuencial.getValorActual(),
                secuencial.isActivo()
        );
    }

    private SecuencialResponse mapearVacio(PuntoEmision puntoEmision, TipoDocumento tipoDocumento) {
        return new SecuencialResponse(
                puntoEmision.getUuid().toString(),
                tipoDocumento.name(),
                0L,
                false
        );
    }
}
