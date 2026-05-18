package com.erp.epmapaApi.repositories;

import com.erp.epmapaApi.Interfaces.FacturasSinCobroInter;
import com.erp.epmapaApi.models.Facturas;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface FacturasR extends JpaRepository<Facturas, Long> {
    @Query(value = "select f.idfactura, sum(rf.cantidad * rf.valorunitario) as subtotal, c.nombre, c.cedula, a.idabonado as cuenta, a.direccionubicacion, f.formapago, e.feccrea, f.fechatransferencia as fectransferencia "
            +
            "from facturas f join rubroxfac rf on f.idfactura = rf.idfactura_facturas " +
            "join clientes c on c.idcliente = f.idcliente " +
            "join lecturas l on l.idfactura = f.idfactura " +
            "join emisiones e on l.idemision = e.idemision " +
            "join abonados a on a.idabonado = f.idabonado " +
            "where f.idabonado = ?1 and (( (f.estado = 1 or f.estado = 2) and f.fechacobro is null) or f.estado = 3 ) and f.fechaconvenio is null and f.fechaeliminacion is null "
            +
            "group by f.idfactura, c.nombre, c.cedula, a.idabonado, a.direccionubicacion, e.feccrea ORDER BY f.idfactura", nativeQuery = true)
    public List<FacturasSinCobroInter> findSincobroDatos(Long cuenta);

    @Query(value = "SELECT CASE WHEN EXISTS (SELECT 1 FROM abonados WHERE idabonado = ?1) THEN TRUE ELSE FALSE END", nativeQuery = true)
    boolean cuentaExist(Long cuenta);

    @Query(value = """
            select
              f.idfactura,
              ROUND(CAST(
                        SUM(
                          CASE
                            WHEN rf.idrubro_rubros = 165 THEN 0
                            WHEN f.swcondonar IS TRUE AND rf.idrubro_rubros = 6 THEN 0
                            ELSE CAST(COALESCE(rf.valorunitario, 0) AS NUMERIC)
                               * CAST(COALESCE(rf.cantidad, 0) AS NUMERIC)
                          END
                        ) AS NUMERIC
                      ), 2) as subtotal,
              f.formapago,
              f.feccrea,
              f.fechatransferencia,
              c.nombre,
              c.cedula,
              a.direccionubicacion as direccion,
                 ROUND(CAST(COALESCE(MAX(ti.interesapagar), 0) AS NUMERIC), 2) AS interes
                                        from facturas f
            join abonados a on f.idabonado = a.idabonado
            join clientes c on a.idresponsable = c.idcliente
            join rubroxfac rf on f.idfactura = rf.idfactura_facturas
            left join tmpinteresxfac ti on ti.idfactura = f.idfactura
            where
              f.idabonado = ?1
              and ( ((f.estado in (1,2)) and f.fechacobro is null) or f.estado = 3 )
              and f.fechaconvenio is null
              and f.fechaeliminacion is null
              and f.feccrea <= CURRENT_DATE
            group by
              f.idfactura, f.formapago, f.feccrea, f.fechatransferencia,
              c.nombre, c.cedula, a.direccionubicacion, ti.interesapagar
            order by f.idfactura;
            
    """, nativeQuery = true)
    public List<FacturasSinCobroInter> findFacturasSinCobro(Long cuenta);
}
