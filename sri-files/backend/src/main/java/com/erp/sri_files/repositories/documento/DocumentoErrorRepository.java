package com.erp.sri_files.repositories.documento;

import com.erp.sri_files.domain.documento.DocumentoError;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface DocumentoErrorRepository extends JpaRepository<DocumentoError, Long> {
    List<DocumentoError> findByDocumento_UuidOrderByCreatedAtDesc(UUID uuid);
}
