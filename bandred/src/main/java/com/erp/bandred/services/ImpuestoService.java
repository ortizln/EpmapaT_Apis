package com.erp.bandred.services;

import com.erp.bandred.interfaces.Factura_int;
import com.erp.bandred.interfaces.NroFactura_int;
import com.erp.bandred.interfaces.Rubroxfaciva_int;
import com.erp.bandred.models.Facturas;
import com.erp.bandred.models.Impuestos;
import com.erp.bandred.repositories.CajasR;
import com.erp.bandred.repositories.FacturasR;
import com.erp.bandred.repositories.ImpuestosR;
import com.erp.bandred.repositories.RubroxfacR;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static com.erp.bandred.services.FacturasService.fPemiCaja;
import static com.erp.bandred.services.FacturasService.fSecuencial;

@Service
public class ImpuestoService {
    @Autowired
    private ImpuestosR im_dao;
    @Autowired
    private RubroxfacR rf_dao ;
    @Autowired
    private CajasR cajasR;
    @Autowired
    private FacturasR dao;
    @Autowired
    private RecaudaxcajaService rxc_service;

    public Map<Long, BigDecimal> calcularIvas(List<Factura_int> facturas) {
        Impuestos impuestoActivo = im_dao.getImpuestoByEstado(true); // 1 sola llamada
        double porcentajeIva = impuestoActivo.getValor() / 100.0;

        return facturas.parallelStream()
                .map(f -> {
                    Long id = f.getIdfactura();
                    List<Rubroxfaciva_int> rubros = rf_dao.getRubrosByIdFactura(id);

                    double iva = rubros.stream()
                            .filter(r -> Boolean.TRUE.equals(r.getIva()))
                            .mapToDouble(r -> r.getCantidad() * r.getValorunitario() * porcentajeIva)
                            .sum();

                    return Map.entry(id, BigDecimal.valueOf(iva).setScale(2, RoundingMode.HALF_UP));
                })
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    public <S extends Facturas> Facturas cobrarFactura(S factura){
        if(factura.getNrofactura() == null){
            NroFactura_int _nroFactura = cajasR.buildNroFactura(factura.getUsuariocobro());
            String secuencial = fSecuencial(_nroFactura.getSecuencial());
            String puntoEmision = fPemiCaja(_nroFactura.getEstablecimiento());
            String codigo = fPemiCaja(_nroFactura.getCodigo());
            String nrofactura = puntoEmision + '-' + codigo +'-'+ secuencial;
            rxc_service.updateLastfactFinFac(factura.getUsuariocobro(), Long.valueOf(secuencial));
            factura.setNrofactura(nrofactura);
        }
        return dao.save(factura);
    }
    public BigDecimal calcularIva(Long idfactura){
        List<Rubroxfaciva_int> rubros = rf_dao.getRubrosByIdFactura(idfactura);
        double iva =0.00;
        Impuestos imp = im_dao.getImpuestoByEstado(true);
        for(Rubroxfaciva_int i: rubros){
            if(i.getIva()){
                iva += (i.getCantidad() * i.getValorunitario()) *((double) imp.getValor() /100);
            }
            else{
                iva += 0.00;
            }
        }
        return BigDecimal.valueOf(iva);

    }
}
