package com.erp.sri_files.repositories.auth;

import com.erp.sri_files.domain.auth.Rol;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface RolRepository extends JpaRepository<Rol, Long> {
    List<Rol> findAllByOrderByCodigoAsc();
    Optional<Rol> findByCodigoIgnoreCase(String codigo);
}
