package com.erp.sri_files.service;

import com.erp.sri_files.dto.request.RolUpdateRequest;
import com.erp.sri_files.dto.response.PermisoResponse;
import com.erp.sri_files.dto.response.RolAuditoriaListadoItemResponse;
import com.erp.sri_files.dto.response.RolAuditoriaListadoResponse;
import com.erp.sri_files.dto.response.RolAuditoriaResponse;
import com.erp.sri_files.dto.response.RolResponse;
import com.erp.sri_files.domain.auth.Permiso;
import com.erp.sri_files.domain.auth.Rol;
import com.erp.sri_files.domain.auth.RolAuditoria;
import com.erp.sri_files.dto.response.UsuarioAutenticadoResponse;
import com.erp.sri_files.exceptions.DocumentoRecepcionException;
import com.erp.sri_files.repositories.auth.PermisoRepository;
import com.erp.sri_files.repositories.auth.RolAuditoriaRepository;
import com.erp.sri_files.repositories.auth.RolRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Service
public class AccessControlService {
    private final RolRepository rolRepository;
    private final PermisoRepository permisoRepository;
    private final RolAuditoriaRepository rolAuditoriaRepository;

    public AccessControlService(
            RolRepository rolRepository,
            PermisoRepository permisoRepository,
            RolAuditoriaRepository rolAuditoriaRepository
    ) {
        this.rolRepository = rolRepository;
        this.permisoRepository = permisoRepository;
        this.rolAuditoriaRepository = rolAuditoriaRepository;
    }

    @Transactional(readOnly = true)
    public List<PermisoResponse> listarPermisos() {
        return permisoRepository.findAllByOrderByCategoriaAscCodigoAsc().stream()
                .map(this::mapPermiso)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<RolResponse> listarRoles() {
        return rolRepository.findAllByOrderByCodigoAsc().stream()
                .map(this::mapRol)
                .toList();
    }

    @Transactional
    public RolResponse actualizarRol(String codigo, RolUpdateRequest request, UsuarioAutenticadoResponse actor) {
        String normalizedCode = codigo.trim().toUpperCase();
        Rol existing = rolRepository.findByCodigoIgnoreCase(normalizedCode)
                .orElseThrow(() -> new DocumentoRecepcionException("No existe rol con codigo " + normalizedCode));
        if (existing == null) {
            throw new DocumentoRecepcionException("No existe rol con codigo " + normalizedCode);
        }

        Set<String> permisosValidos = permisoRepository.findAll().stream()
                .map(Permiso::getCodigo)
                .collect(java.util.stream.Collectors.toSet());

        List<String> permisosNormalizados = request.permisos().stream()
                .map(value -> value.trim().toUpperCase())
                .distinct()
                .toList();

        List<String> permisosInvalidos = permisosNormalizados.stream()
                .filter(value -> !permisosValidos.contains(value))
                .toList();
        if (!permisosInvalidos.isEmpty()) {
            throw new DocumentoRecepcionException("Permisos invalidos para el rol " + normalizedCode + ": " + String.join(", ", permisosInvalidos));
        }

        if ("ADMIN".equals(normalizedCode)) {
            List<String> permisosCriticos = List.of("ROL_VER", "ROL_ADMINISTRAR", "USUARIO_VER", "USUARIO_EDITAR");
            boolean faltanCriticos = permisosCriticos.stream().anyMatch(value -> !permisosNormalizados.contains(value));
            if (faltanCriticos) {
                throw new DocumentoRecepcionException("El rol ADMIN debe conservar los permisos criticos de seguridad");
            }
        }

        List<Permiso> permisosSeleccionados = permisoRepository.findByCodigoIn(permisosNormalizados);
        String nombreAnterior = existing.getNombre();
        String descripcionAnterior = existing.getDescripcion();
        List<String> permisosAnteriores = existing.getPermisos().stream()
                .map(Permiso::getCodigo)
                .sorted()
                .toList();

        existing.setNombre(request.nombre().trim());
        existing.setDescripcion(request.descripcion().trim());
        existing.setPermisos(new LinkedHashSet<>(permisosSeleccionados));
        Rol guardado = rolRepository.save(existing);

        registrarAuditoria(
                guardado,
                actor,
                "ROL_ACTUALIZADO",
                construirDescripcionAuditoria(
                        guardado.getCodigo(),
                        nombreAnterior,
                        guardado.getNombre(),
                        descripcionAnterior,
                        guardado.getDescripcion(),
                        permisosAnteriores,
                        permisosNormalizados
                )
        );

        return mapRol(guardado);
    }

    @Transactional(readOnly = true)
    public List<RolAuditoriaResponse> obtenerAuditoria(String codigo) {
        Rol rol = rolRepository.findByCodigoIgnoreCase(codigo)
                .orElseThrow(() -> new DocumentoRecepcionException("No existe rol con codigo " + codigo));

        return rolAuditoriaRepository.findTop20ByRolOrderByCreatedAtDesc(rol).stream()
                .map(item -> new RolAuditoriaResponse(
                        item.getAccion(),
                        item.getDescripcion(),
                        item.getActorUsername(),
                        item.getCreatedAt().toString()
                ))
                .toList();
    }

    @Transactional(readOnly = true)
    public RolAuditoriaListadoResponse listarAuditoriaReciente(int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<RolAuditoria> auditoria = rolAuditoriaRepository.findAllByOrderByCreatedAtDesc(pageable);

        return new RolAuditoriaListadoResponse(
                auditoria.getContent().stream().map(item -> new RolAuditoriaListadoItemResponse(
                        item.getId(),
                        item.getRol() != null ? item.getRol().getCodigo() : null,
                        item.getRol() != null ? item.getRol().getNombre() : null,
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

    @Transactional(readOnly = true)
    public List<String> obtenerPermisosPorRol(String rol) {
        return rolRepository.findByCodigoIgnoreCase(rol)
                .map(Rol::getPermisos)
                .map(permisos -> permisos.stream()
                        .map(Permiso::getCodigo)
                        .sorted()
                        .toList())
                .orElse(List.of());
    }

    private PermisoResponse mapPermiso(Permiso permiso) {
        return new PermisoResponse(
                permiso.getCodigo(),
                permiso.getNombre(),
                permiso.getDescripcion(),
                permiso.getCategoria()
        );
    }

    private RolResponse mapRol(Rol rol) {
        return new RolResponse(
                rol.getCodigo(),
                rol.getNombre(),
                rol.getDescripcion(),
                rol.getPermisos().stream()
                        .map(Permiso::getCodigo)
                        .sorted()
                        .toList()
        );
    }

    private void registrarAuditoria(Rol rol, UsuarioAutenticadoResponse actor, String accion, String descripcion) {
        RolAuditoria auditoria = new RolAuditoria();
        auditoria.setRol(rol);
        auditoria.setActorUuid(actor != null ? java.util.UUID.fromString(actor.id()) : null);
        auditoria.setActorUsername(actor != null ? actor.nombre() : "sistema");
        auditoria.setAccion(accion);
        auditoria.setDescripcion(descripcion);
        auditoria.setCreatedAt(LocalDateTime.now());
        rolAuditoriaRepository.save(auditoria);
    }

    private String construirDescripcionAuditoria(
            String codigo,
            String nombreAnterior,
            String nombreNuevo,
            String descripcionAnterior,
            String descripcionNueva,
            List<String> permisosAnteriores,
            List<String> permisosNuevos
    ) {
        boolean cambioNombre = !nombreAnterior.equals(nombreNuevo);
        boolean cambioDescripcion = !descripcionAnterior.equals(descripcionNueva);
        boolean cambioPermisos = !permisosAnteriores.equals(permisosNuevos);

        StringBuilder builder = new StringBuilder("Rol ").append(codigo).append(" actualizado");
        if (cambioNombre) {
            builder.append("; nombre: ").append(nombreAnterior).append(" -> ").append(nombreNuevo);
        }
        if (cambioDescripcion) {
            builder.append("; descripcion actualizada");
        }
        if (cambioPermisos) {
            builder.append("; permisos: ")
                    .append(String.join(", ", permisosAnteriores))
                    .append(" -> ")
                    .append(String.join(", ", permisosNuevos));
        }
        return builder.toString();
    }
}
