package com.erp.sri_files.repositories.documento;

import com.erp.sri_files.domain.documento.ProductoCatalogo;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ProductoCatalogoRepository extends JpaRepository<ProductoCatalogo, Long> {
    List<ProductoCatalogo> findByEmpresa_UuidOrderByNombreAsc(UUID empresaUuid);
    Optional<ProductoCatalogo> findByUuid(UUID uuid);
    Optional<ProductoCatalogo> findByEmpresa_UuidAndCodigo(UUID empresaUuid, String codigo);
}
