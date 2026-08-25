package com.erp.sri_files.repositories.documento;

import com.erp.sri_files.domain.documento.ClienteCatalogo;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ClienteCatalogoRepository extends JpaRepository<ClienteCatalogo, Long> {
    List<ClienteCatalogo> findByEmpresa_UuidOrderByRazonSocialAsc(UUID empresaUuid);
    Optional<ClienteCatalogo> findByUuid(UUID uuid);
    Optional<ClienteCatalogo> findByEmpresa_UuidAndIdentificacion(UUID empresaUuid, String identificacion);
}
