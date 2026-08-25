package com.erp.sri_files.repositories.documento;

import com.erp.sri_files.domain.documento.Empresa;
import com.erp.sri_files.domain.documento.RecursoEmpresa;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface RecursoEmpresaRepository extends JpaRepository<RecursoEmpresa, Long> {
    List<RecursoEmpresa> findByEmpresaOrderByCreatedAtDesc(Empresa empresa);
    Optional<RecursoEmpresa> findByUuid(UUID uuid);
}
