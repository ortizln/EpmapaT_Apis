package com.erp.sri_files.repositories.documento;

import com.erp.sri_files.domain.documento.Empresa;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Optional;
import java.util.UUID;

public interface EmpresaRepository extends JpaRepository<Empresa, Long> {
    Optional<Empresa> findByUuid(UUID uuid);
    Optional<Empresa> findByRuc(String ruc);
    Page<Empresa> findAllByOrderByRazonSocialAsc(Pageable pageable);
}
