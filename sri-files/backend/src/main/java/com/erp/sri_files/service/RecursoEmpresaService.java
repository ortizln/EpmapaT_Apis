package com.erp.sri_files.service;

import com.erp.sri_files.domain.documento.Empresa;
import com.erp.sri_files.domain.documento.RecursoEmpresa;
import com.erp.sri_files.domain.documento.RecursoEmpresaTipo;
import com.erp.sri_files.dto.request.RecursoEmpresaEstadoRequest;
import com.erp.sri_files.dto.response.RecursoEmpresaResponse;
import com.erp.sri_files.dto.response.UsuarioAutenticadoResponse;
import com.erp.sri_files.exceptions.DocumentoRecepcionException;
import com.erp.sri_files.repositories.documento.EmpresaRepository;
import com.erp.sri_files.repositories.documento.RecursoEmpresaRepository;
import com.erp.sri_files.storage.StorageService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

@Service
public class RecursoEmpresaService {

    private final RecursoEmpresaRepository recursoEmpresaRepository;
    private final EmpresaRepository empresaRepository;
    private final StorageService storageService;
    private final EmpresaService empresaService;

    public RecursoEmpresaService(
            RecursoEmpresaRepository recursoEmpresaRepository,
            EmpresaRepository empresaRepository,
            StorageService storageService,
            EmpresaService empresaService
    ) {
        this.recursoEmpresaRepository = recursoEmpresaRepository;
        this.empresaRepository = empresaRepository;
        this.storageService = storageService;
        this.empresaService = empresaService;
    }

    @Transactional(readOnly = true)
    public List<RecursoEmpresaResponse> listar(UUID empresaId) {
        Empresa empresa = buscarEmpresa(empresaId);
        return recursoEmpresaRepository.findByEmpresaOrderByCreatedAtDesc(empresa).stream()
                .map(this::mapear)
                .toList();
    }

    @Transactional
    public RecursoEmpresaResponse crear(
            UUID empresaId,
            RecursoEmpresaTipo tipo,
            String nombre,
            MultipartFile file,
            UsuarioAutenticadoResponse actor
    ) {
        Empresa empresa = buscarEmpresa(empresaId);
        if (tipo == null) {
            throw new DocumentoRecepcionException("El tipo de recurso es obligatorio");
        }
        if (file == null || file.isEmpty()) {
            throw new DocumentoRecepcionException("El archivo del recurso es obligatorio");
        }

        String fileName = file.getOriginalFilename() == null ? "recurso.bin" : file.getOriginalFilename();
        String path = storageService.buildArchivoPath(
                "empresa-recursos",
                empresa.getRuc(),
                empresa.getUuid().toString() + "/" + tipo.name().toLowerCase(),
                UUID.randomUUID() + "-" + fileName
        );

        try {
            storageService.saveBytes(path, file.getBytes());
        } catch (IOException ex) {
            throw new DocumentoRecepcionException("No fue posible guardar el archivo del recurso");
        }

        RecursoEmpresa recurso = new RecursoEmpresa();
        recurso.setUuid(UUID.randomUUID());
        recurso.setEmpresa(empresa);
        recurso.setTipo(tipo);
        recurso.setNombre(normalizar(nombre != null ? nombre : tipo.name()));
        recurso.setNombreArchivo(fileName);
        recurso.setMimeType(file.getContentType());
        recurso.setRuta(path);
        recurso.setActivo(true);
        recursoEmpresaRepository.save(recurso);

        empresaService.registrarAuditoriaConfiguracion(empresa, actor, "RECURSO_EMPRESA_CREADO",
                "Recurso " + tipo.name() + " cargado para " + empresa.getRazonSocial());
        return mapear(recurso);
    }

    @Transactional
    public RecursoEmpresaResponse actualizarEstado(UUID uuid, RecursoEmpresaEstadoRequest request, UsuarioAutenticadoResponse actor) {
        RecursoEmpresa recurso = recursoEmpresaRepository.findByUuid(uuid)
                .orElseThrow(() -> new DocumentoRecepcionException("No existe recurso con uuid " + uuid));
        recurso.setActivo(Boolean.TRUE.equals(request.activo()));
        recursoEmpresaRepository.save(recurso);
        empresaService.registrarAuditoriaConfiguracion(recurso.getEmpresa(), actor, "RECURSO_EMPRESA_ESTADO_ACTUALIZADO",
                "Recurso " + recurso.getTipo().name() + " " + (recurso.isActivo() ? "activado" : "desactivado"));
        return mapear(recurso);
    }

    private Empresa buscarEmpresa(UUID empresaId) {
        return empresaRepository.findByUuid(empresaId)
                .orElseThrow(() -> new DocumentoRecepcionException("No existe empresa con uuid " + empresaId));
    }

    private RecursoEmpresaResponse mapear(RecursoEmpresa recurso) {
        return new RecursoEmpresaResponse(
                recurso.getUuid().toString(),
                recurso.getEmpresa().getUuid().toString(),
                recurso.getTipo().name(),
                recurso.getNombre(),
                recurso.getNombreArchivo(),
                recurso.getMimeType(),
                recurso.isActivo(),
                recurso.getCreatedAt().toString()
        );
    }

    private String normalizar(String value) {
        if (value == null) {
            return null;
        }
        String limpio = value.trim();
        return limpio.isEmpty() ? null : limpio;
    }
}
