package com.erp.sri_files.repositories.documento;

import com.erp.sri_files.domain.documento.DocumentoArchivo;
import com.erp.sri_files.domain.documento.DocumentoArchivoTipo;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface DocumentoArchivoRepository extends JpaRepository<DocumentoArchivo, Long> {
    List<DocumentoArchivo> findByDocumento_UuidAndActivoTrueOrderByCreatedAtDesc(UUID documentoUuid);
    List<DocumentoArchivo> findByDocumento_UuidInAndActivoTrue(List<UUID> documentoUuids);
    Optional<DocumentoArchivo> findFirstByDocumento_UuidAndTipoArchivoAndActivoTrueOrderByCreatedAtDesc(
            UUID documentoUuid,
            DocumentoArchivoTipo tipoArchivo
    );
}
