package com.erp.sri_files.repositories.documento;

import com.erp.sri_files.domain.documento.Establecimiento;
import com.erp.sri_files.domain.documento.PuntoEmision;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PuntoEmisionRepository extends JpaRepository<PuntoEmision, Long> {
    List<PuntoEmision> findByEstablecimientoOrderByCodigoAsc(Establecimiento establecimiento);
    Optional<PuntoEmision> findByUuid(UUID uuid);
    Optional<PuntoEmision> findByEstablecimientoAndCodigo(Establecimiento establecimiento, String codigo);
}
