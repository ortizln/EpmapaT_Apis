package com.erp.epmapaApi.repositories;

import com.erp.epmapaApi.models.Lecturas;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface LecturasR extends JpaRepository<Lecturas, Long> {

    @Query(value = """
        SELECT l.idfactura, e.feccrea AS periodo, l.lecturaanterior, l.lecturaactual,
               (COALESCE(l.lecturaactual, 0) - COALESCE(l.lecturaanterior, 0)) AS consumo,
               c.descripcion AS categoria,
               f.pagado, f.fechacobro, f.formapago, f.totaltarifa,
               m.descripcion AS modulo,
               fp.descripcion AS forma_pago_desc
        FROM lecturas l
        JOIN emisiones e ON l.idemision = e.idemision
        JOIN facturas f ON l.idfactura = f.idfactura
        LEFT JOIN categorias c ON l.idcategoria = c.idcategoria
        LEFT JOIN modulos m ON f.idmodulo = m.idmodulo
        LEFT JOIN formacobro fp ON f.formapago = fp.idformacobro
        WHERE l.idabonado_abonados = ?1
        ORDER BY e.feccrea DESC
        LIMIT ?2
    """, nativeQuery = true)
    List<Object[]> findHistorialByAbonado(Long idabonado, int limite);

    @Query("SELECT l FROM Lecturas l LEFT JOIN FETCH l.idnovedad_novedades WHERE l.idfactura = :idfactura")
    List<Lecturas> findByIdfacturaWithNovedad(Long idfactura);
}
