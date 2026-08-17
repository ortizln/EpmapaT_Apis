package com.erp.sri_files.repositories.auth;

import com.erp.sri_files.domain.auth.Permiso;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface PermisoRepository extends JpaRepository<Permiso, Long> {
    List<Permiso> findAllByOrderByCategoriaAscCodigoAsc();
    List<Permiso> findByCodigoIn(Collection<String> codigos);
    Optional<Permiso> findByCodigoIgnoreCase(String codigo);
}
