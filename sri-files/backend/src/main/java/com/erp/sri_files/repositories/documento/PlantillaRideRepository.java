package com.erp.sri_files.repositories.documento;

import com.erp.sri_files.domain.documento.Empresa;
import com.erp.sri_files.domain.documento.PlantillaRide;
import com.erp.sri_files.domain.documento.TipoDocumento;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PlantillaRideRepository extends JpaRepository<PlantillaRide, Long> {
    List<PlantillaRide> findByEmpresaOrderByTipoDocumentoAscVersionDesc(Empresa empresa);
    Optional<PlantillaRide> findByUuid(UUID uuid);
    List<PlantillaRide> findByEmpresaAndTipoDocumento(Empresa empresa, TipoDocumento tipoDocumento);
    Optional<PlantillaRide> findFirstByEmpresaAndTipoDocumentoAndPredeterminadaTrueAndActivaTrue(Empresa empresa, TipoDocumento tipoDocumento);
}
