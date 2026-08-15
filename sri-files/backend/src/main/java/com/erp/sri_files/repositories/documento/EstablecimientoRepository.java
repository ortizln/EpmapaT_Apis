package com.erp.sri_files.repositories.documento;

import com.erp.sri_files.domain.documento.Empresa;
import com.erp.sri_files.domain.documento.Establecimiento;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface EstablecimientoRepository extends JpaRepository<Establecimiento, Long> {
    List<Establecimiento> findByEmpresaOrderByCodigoAsc(Empresa empresa);
    Optional<Establecimiento> findByUuid(UUID uuid);
    Optional<Establecimiento> findByEmpresaAndCodigo(Empresa empresa, String codigo);
}
