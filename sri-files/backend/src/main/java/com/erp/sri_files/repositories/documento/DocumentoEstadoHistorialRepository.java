package com.erp.sri_files.repositories.documento;

import com.erp.sri_files.domain.documento.DocumentoEstadoHistorial;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface DocumentoEstadoHistorialRepository extends JpaRepository<DocumentoEstadoHistorial, Long> {
    List<DocumentoEstadoHistorial> findByDocumento_UuidOrderByCreatedAtDesc(UUID uuid);
    List<DocumentoEstadoHistorial> findTop100ByOrderByCreatedAtDesc();
}
