package com.erp.bandred.repositories;

import com.erp.bandred.interfaces.FacturasCobradas;
import com.erp.bandred.interfaces.FacturasSinCobroInter;
import com.erp.bandred.models.Facturas;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

public interface FacturasR extends JpaRepository<Facturas, Long> {
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
    @Query(value = """
    select
        f.idfactura
    from facturas f
    where
        f.idabonado = ?1
        and (((f.estado = 1 or f.estado = 2) and f.fechacobro is null)
             or f.estado = 3)
        and f.fechaconvenio is null
        and f.fechaeliminacion is null and f.totaltarifa > 0
        and f.feccrea <= CURRENT_DATE
    order by
        f.idfactura;
    """, nativeQuery = true)
    List<Long> findPlanillas(Long cuenta);

    @Query(value = """
    select
        CEIL(count(rf.idrubro_rubros) * 100) / 100 as nrubros,
        CEIL(sum(rf.cantidad * rf.valorunitario) * 100) / 100 as valortotal,
        f.idfactura as planilla,
        f.nrofactura,
        f.secuencialfacilito,
        f.fechacobro,
        f.horacobro,
        u.nomusu as usuario,
        c.nombre
    from
        facturas f
    join rubroxfac rf
        on f.idfactura = rf.idfactura_facturas
    join usuarios u
        on f.usuariocobro = u.idusuario
    join clientes c
        on f.idcliente = c.idcliente
    where
        f.usuariocobro = ?1
        and f.fechacobro between ?2 and ?3
        and f.horacobro between ?4 and ?5
    group by
        f.idfactura,
        u.nomusu,
        c.nombre
    order by
        f.idfactura
    """, nativeQuery = true)
    public List<FacturasCobradas> getReporteFacturasCobradas(
            Long idusuario,
            LocalDate df,
            LocalDate hf,
            LocalTime dh,
            LocalTime hh);

    @Query(value = "SELECT CASE WHEN EXISTS (SELECT 1 FROM abonados WHERE idabonado = ?1) THEN TRUE ELSE FALSE END", nativeQuery = true)
    boolean cuentaExist(Long cuenta);



}
