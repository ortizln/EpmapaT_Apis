package com.erp.sri_files.repositories.auth;

import com.erp.sri_files.domain.auth.UsuarioSistema;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Optional;
import java.util.UUID;

public interface UsuarioSistemaRepository extends JpaRepository<UsuarioSistema, Long> {
    Optional<UsuarioSistema> findByUsernameIgnoreCase(String username);
    Optional<UsuarioSistema> findByCorreoIgnoreCase(String correo);
    Optional<UsuarioSistema> findByUuid(UUID uuid);
    Page<UsuarioSistema> findAllByOrderByNombreAsc(Pageable pageable);
}
