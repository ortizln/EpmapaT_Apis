package com.erp.sri_files.repositories.documento;

import com.erp.sri_files.domain.documento.PuntoEmision;
import com.erp.sri_files.domain.documento.Secuencial;
import com.erp.sri_files.domain.documento.TipoDocumento;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SecuencialRepository extends JpaRepository<Secuencial, Long> {
    List<Secuencial> findByPuntoEmisionOrderByTipoDocumentoAsc(PuntoEmision puntoEmision);
    Optional<Secuencial> findByPuntoEmisionAndTipoDocumento(PuntoEmision puntoEmision, TipoDocumento tipoDocumento);
}
