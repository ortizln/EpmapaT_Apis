package com.erp.sri_files.service;

import com.erp.sri_files.domain.auth.EmpresaAuditoria;
import com.erp.sri_files.domain.auth.RolAuditoria;
import com.erp.sri_files.domain.auth.UsuarioAuditoria;
import com.erp.sri_files.domain.documento.DocumentoEstadoHistorial;
import com.erp.sri_files.dto.response.AuditoriaItemResponse;
import com.erp.sri_files.dto.response.AuditoriaListadoResponse;
import com.erp.sri_files.exceptions.DocumentoRecepcionException;
import com.erp.sri_files.repositories.auth.EmpresaAuditoriaRepository;
import com.erp.sri_files.repositories.auth.RolAuditoriaRepository;
import com.erp.sri_files.repositories.auth.UsuarioAuditoriaRepository;
import com.erp.sri_files.repositories.documento.DocumentoEstadoHistorialRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Service
public class AuditoriaService {

    private final DocumentoEstadoHistorialRepository documentoEstadoHistorialRepository;
    private final UsuarioAuditoriaRepository usuarioAuditoriaRepository;
    private final RolAuditoriaRepository rolAuditoriaRepository;
    private final EmpresaAuditoriaRepository empresaAuditoriaRepository;

    public AuditoriaService(
            DocumentoEstadoHistorialRepository documentoEstadoHistorialRepository,
            UsuarioAuditoriaRepository usuarioAuditoriaRepository,
            RolAuditoriaRepository rolAuditoriaRepository,
            EmpresaAuditoriaRepository empresaAuditoriaRepository
    ) {
        this.documentoEstadoHistorialRepository = documentoEstadoHistorialRepository;
        this.usuarioAuditoriaRepository = usuarioAuditoriaRepository;
        this.rolAuditoriaRepository = rolAuditoriaRepository;
        this.empresaAuditoriaRepository = empresaAuditoriaRepository;
    }

    @Transactional(readOnly = true)
    public AuditoriaListadoResponse listar(int page, int size) {
        List<AuditoriaItemResponse> eventos = new ArrayList<>();

        documentoEstadoHistorialRepository.findTop100ByOrderByCreatedAtDesc()
                .forEach(item -> eventos.add(mapDocumento(item)));
        usuarioAuditoriaRepository.findAllByOrderByCreatedAtDesc(org.springframework.data.domain.PageRequest.of(0, 100))
                .forEach(item -> eventos.add(mapUsuario(item)));
        rolAuditoriaRepository.findAllByOrderByCreatedAtDesc(org.springframework.data.domain.PageRequest.of(0, 100))
                .forEach(item -> eventos.add(mapRol(item)));
        empresaAuditoriaRepository.findAllByOrderByCreatedAtDesc(org.springframework.data.domain.PageRequest.of(0, 100))
                .forEach(item -> eventos.add(mapEmpresa(item)));

        List<AuditoriaItemResponse> ordenados = eventos.stream()
                .sorted(Comparator.comparing(
                        AuditoriaItemResponse::fecha,
                        Comparator.nullsLast(Comparator.reverseOrder())
                ))
                .toList();

        int fromIndex = Math.min(page * size, ordenados.size());
        int toIndex = Math.min(fromIndex + size, ordenados.size());
        int totalPages = size <= 0 ? 1 : (int) Math.ceil((double) ordenados.size() / size);

        return new AuditoriaListadoResponse(
                ordenados.subList(fromIndex, toIndex),
                page,
                size,
                ordenados.size(),
                totalPages
        );
    }

    @Transactional(readOnly = true)
    public AuditoriaItemResponse obtener(String id) {
        if (id == null || id.isBlank() || !id.contains("-")) {
            throw new DocumentoRecepcionException("Identificador de auditoria invalido");
        }

        String prefix = id.substring(0, id.indexOf('-')).toUpperCase();
        Long internalId = Long.parseLong(id.substring(id.indexOf('-') + 1));

        return switch (prefix) {
            case "DOC" -> mapDocumento(documentoEstadoHistorialRepository.findById(internalId)
                    .orElseThrow(() -> new DocumentoRecepcionException("No existe auditoria " + id)));
            case "USR" -> mapUsuario(usuarioAuditoriaRepository.findById(internalId)
                    .orElseThrow(() -> new DocumentoRecepcionException("No existe auditoria " + id)));
            case "ROL" -> mapRol(rolAuditoriaRepository.findById(internalId)
                    .orElseThrow(() -> new DocumentoRecepcionException("No existe auditoria " + id)));
            case "EMP" -> mapEmpresa(empresaAuditoriaRepository.findById(internalId)
                    .orElseThrow(() -> new DocumentoRecepcionException("No existe auditoria " + id)));
            default -> throw new DocumentoRecepcionException("Prefijo de auditoria no soportado");
        };
    }

    private AuditoriaItemResponse mapDocumento(DocumentoEstadoHistorial item) {
        return new AuditoriaItemResponse(
                "DOC-" + item.getId(),
                "DOCUMENTO",
                item.getDocumento() != null && item.getDocumento().getUuid() != null ? item.getDocumento().getUuid().toString() : null,
                item.getEstadoNuevo() != null ? item.getEstadoNuevo().name() : null,
                item.getDescripcion(),
                item.getOrigen(),
                toText(item.getCreatedAt())
        );
    }

    private AuditoriaItemResponse mapUsuario(UsuarioAuditoria item) {
        return new AuditoriaItemResponse(
                "USR-" + item.getId(),
                "USUARIO",
                item.getUsuario() != null && item.getUsuario().getUuid() != null ? item.getUsuario().getUuid().toString() : null,
                item.getAccion(),
                item.getDescripcion(),
                item.getActorUsername(),
                toText(item.getCreatedAt())
        );
    }

    private AuditoriaItemResponse mapRol(RolAuditoria item) {
        return new AuditoriaItemResponse(
                "ROL-" + item.getId(),
                "ROL",
                item.getRol() != null ? item.getRol().getCodigo() : null,
                item.getAccion(),
                item.getDescripcion(),
                item.getActorUsername(),
                toText(item.getCreatedAt())
        );
    }

    private AuditoriaItemResponse mapEmpresa(EmpresaAuditoria item) {
        return new AuditoriaItemResponse(
                "EMP-" + item.getId(),
                "EMPRESA",
                item.getEmpresa() != null && item.getEmpresa().getUuid() != null ? item.getEmpresa().getUuid().toString() : null,
                item.getAccion(),
                item.getDescripcion(),
                item.getActorUsername(),
                toText(item.getCreatedAt())
        );
    }

    private String toText(LocalDateTime value) {
        return value != null ? value.toString() : null;
    }
}
