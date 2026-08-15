package com.erp.sri_files.repositories.documento;

import com.erp.sri_files.domain.documento.DocumentoElectronico;
import com.erp.sri_files.domain.documento.DocumentoEstado;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface DocumentoElectronicoRepository extends JpaRepository<DocumentoElectronico, Long>, JpaSpecificationExecutor<DocumentoElectronico> {
    Optional<DocumentoElectronico> findByUuid(UUID uuid);
    Optional<DocumentoElectronico> findByEmpresaIdAndExternalId(Long empresaId, String externalId);
    Optional<DocumentoElectronico> findByEmpresaIdAndIdempotencyKey(Long empresaId, String idempotencyKey);
    List<DocumentoElectronico> findTop20ByEstadoActualOrderByFechaRecepcionAsc(DocumentoEstado estadoActual);
    Page<DocumentoElectronico> findAll(Pageable pageable);
    boolean existsByEmpresaId(Long empresaId);
}
