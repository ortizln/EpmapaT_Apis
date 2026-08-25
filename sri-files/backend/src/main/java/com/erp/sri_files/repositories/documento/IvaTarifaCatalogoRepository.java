package com.erp.sri_files.repositories.documento;

import com.erp.sri_files.domain.documento.IvaTarifaCatalogo;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface IvaTarifaCatalogoRepository extends JpaRepository<IvaTarifaCatalogo, Long> {
    List<IvaTarifaCatalogo> findByEmpresa_UuidOrderByPorcentajeAscNombreAsc(UUID empresaUuid);
    Optional<IvaTarifaCatalogo> findByUuid(UUID uuid);
    Optional<IvaTarifaCatalogo> findByEmpresa_UuidAndCodigo(UUID empresaUuid, String codigo);
}
