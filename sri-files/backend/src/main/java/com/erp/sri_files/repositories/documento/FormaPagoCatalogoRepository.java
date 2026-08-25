package com.erp.sri_files.repositories.documento;

import com.erp.sri_files.domain.documento.FormaPagoCatalogo;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface FormaPagoCatalogoRepository extends JpaRepository<FormaPagoCatalogo, Long> {
    List<FormaPagoCatalogo> findByEmpresa_UuidOrderByNombreAsc(UUID empresaUuid);
    Optional<FormaPagoCatalogo> findByUuid(UUID uuid);
    Optional<FormaPagoCatalogo> findByEmpresa_UuidAndCodigo(UUID empresaUuid, String codigo);
}
