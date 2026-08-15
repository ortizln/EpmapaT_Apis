package com.erp.sri_files.service;

import com.erp.sri_files.domain.auth.UsuarioSistema;
import com.erp.sri_files.domain.auth.UsuarioAuditoria;
import com.erp.sri_files.dto.request.UsuarioCrearRequest;
import com.erp.sri_files.dto.request.UsuarioEstadoRequest;
import com.erp.sri_files.dto.request.UsuarioPasswordResetRequest;
import com.erp.sri_files.dto.response.UsuarioAutenticadoResponse;
import com.erp.sri_files.dto.response.UsuarioAuditoriaResponse;
import com.erp.sri_files.dto.response.UsuarioSistemaListadoResponse;
import com.erp.sri_files.dto.response.UsuarioSistemaResponse;
import com.erp.sri_files.exceptions.DocumentoRecepcionException;
import com.erp.sri_files.repositories.auth.UsuarioAuditoriaRepository;
import com.erp.sri_files.repositories.auth.UsuarioSistemaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
public class UsuarioSistemaService {

    private final UsuarioSistemaRepository usuarioSistemaRepository;
    private final UsuarioAuditoriaRepository usuarioAuditoriaRepository;
    private final PasswordHashService passwordHashService;

    public UsuarioSistemaService(
            UsuarioSistemaRepository usuarioSistemaRepository,
            UsuarioAuditoriaRepository usuarioAuditoriaRepository,
            PasswordHashService passwordHashService
    ) {
        this.usuarioSistemaRepository = usuarioSistemaRepository;
        this.usuarioAuditoriaRepository = usuarioAuditoriaRepository;
        this.passwordHashService = passwordHashService;
    }

    @Transactional(readOnly = true)
    public UsuarioSistemaListadoResponse listar(int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<UsuarioSistema> usuarios = usuarioSistemaRepository.findAllByOrderByNombreAsc(pageable);
        return new UsuarioSistemaListadoResponse(
                usuarios.getContent().stream().map(this::mapear).toList(),
                usuarios.getNumber(),
                usuarios.getSize(),
                usuarios.getTotalElements(),
                usuarios.getTotalPages()
        );
    }

    @Transactional
    public UsuarioSistemaResponse crear(UsuarioCrearRequest request, UsuarioAutenticadoResponse actor) {
        if (usuarioSistemaRepository.findByUsernameIgnoreCase(request.username()).isPresent()) {
            throw new DocumentoRecepcionException("Ya existe un usuario con username " + request.username());
        }

        UsuarioSistema usuario = new UsuarioSistema();
        usuario.setUuid(UUID.randomUUID());
        usuario.setUsername(request.username().trim());
        usuario.setNombre(request.nombre().trim());
        usuario.setCorreo(request.correo().trim().toLowerCase());
        usuario.setRol(request.rol().trim().toUpperCase());
        usuario.setActivo(true);

        String salt = passwordHashService.generarSalt();
        usuario.setPasswordSalt(salt);
        usuario.setPasswordHash(passwordHashService.hash(request.password(), salt));

        UsuarioSistema guardado = usuarioSistemaRepository.save(usuario);
        registrarAuditoria(
                guardado,
                actor,
                actor.nombre(),
                "USUARIO_CREADO",
                "Usuario creado con rol " + guardado.getRol()
        );
        return mapear(guardado);
    }

    @Transactional
    public UsuarioSistemaResponse actualizarEstado(
            UUID uuid,
            UsuarioEstadoRequest request,
            UsuarioAutenticadoResponse actor
    ) {
        UsuarioSistema usuario = buscarPorUuid(uuid);
        boolean mismoUsuario = actor.id().equals(usuario.getUuid().toString());
        boolean actorEsAdmin = actor.roles().contains("ADMIN");

        if (mismoUsuario && actorEsAdmin && !request.activo()) {
            throw new DocumentoRecepcionException("No puedes desactivar tu propio usuario administrador");
        }

        usuario.setActivo(request.activo());
        UsuarioSistema guardado = usuarioSistemaRepository.save(usuario);
        registrarAuditoria(
                guardado,
                actor,
                actor.nombre(),
                request.activo() ? "USUARIO_ACTIVADO" : "USUARIO_DESACTIVADO",
                request.activo()
                        ? "Usuario activado desde administracion"
                        : "Usuario desactivado desde administracion"
        );
        return mapear(guardado);
    }

    @Transactional
    public UsuarioSistemaResponse resetearPassword(
            UUID uuid,
            UsuarioPasswordResetRequest request,
            UsuarioAutenticadoResponse actor
    ) {
        UsuarioSistema usuario = buscarPorUuid(uuid);
        String salt = passwordHashService.generarSalt();
        usuario.setPasswordSalt(salt);
        usuario.setPasswordHash(passwordHashService.hash(request.password(), salt));
        UsuarioSistema guardado = usuarioSistemaRepository.save(usuario);
        registrarAuditoria(
                guardado,
                actor,
                actor.nombre(),
                "PASSWORD_RESETEADA",
                "Contrasena actualizada desde administracion"
        );
        return mapear(guardado);
    }

    @Transactional(readOnly = true)
    public List<UsuarioAuditoriaResponse> obtenerAuditoria(UUID uuid) {
        UsuarioSistema usuario = buscarPorUuid(uuid);
        return usuarioAuditoriaRepository.findTop20ByUsuarioOrderByCreatedAtDesc(usuario).stream()
                .map(item -> new UsuarioAuditoriaResponse(
                        item.getAccion(),
                        item.getDescripcion(),
                        item.getActorUsername(),
                        item.getCreatedAt().toString()
                ))
                .toList();
    }

    private UsuarioSistema buscarPorUuid(UUID uuid) {
        return usuarioSistemaRepository.findByUuid(uuid)
                .orElseThrow(() -> new DocumentoRecepcionException("No existe usuario con uuid " + uuid));
    }

    private void registrarAuditoria(
            UsuarioSistema usuario,
            UsuarioAutenticadoResponse actor,
            String actorUsername,
            String accion,
            String descripcion
    ) {
        UsuarioAuditoria auditoria = new UsuarioAuditoria();
        auditoria.setUsuario(usuario);
        auditoria.setActorUuid(actor != null ? UUID.fromString(actor.id()) : null);
        auditoria.setActorUsername(actor != null ? actor.nombre() : actorUsername);
        auditoria.setAccion(accion);
        auditoria.setDescripcion(descripcion);
        auditoria.setCreatedAt(LocalDateTime.now());
        usuarioAuditoriaRepository.save(auditoria);
    }

    private UsuarioSistemaResponse mapear(UsuarioSistema usuario) {
        return new UsuarioSistemaResponse(
                usuario.getUuid().toString(),
                usuario.getUsername(),
                usuario.getNombre(),
                usuario.getCorreo(),
                usuario.getRol(),
                usuario.isActivo()
        );
    }
}
