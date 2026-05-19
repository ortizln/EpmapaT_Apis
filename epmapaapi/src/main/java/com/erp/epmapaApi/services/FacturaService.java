package com.erp.epmapaApi.services;

import com.erp.epmapaApi.DTO.FacturaDTO;
import com.erp.epmapaApi.Interfaces.FacturasSinCobroInter;
import com.erp.epmapaApi.models.Lecturas;
import com.erp.epmapaApi.models.Rubroxfac;
import com.erp.epmapaApi.repositories.FacturasR;
import com.erp.epmapaApi.repositories.LecturasR;
import com.erp.epmapaApi.repositories.RubroxfacR;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

@RequiredArgsConstructor
@Service
public class FacturaService {
    private final FacturasR dao;
    private final LecturasR lecturasR;
    private final RubroxfacR rubroxfacR;

    private void validateInput(Long cuenta) {
        if (cuenta == null) {
            throw new IllegalArgumentException("Los parámetros 'user' y 'cuenta' no pueden ser nulos");
        }
    }
    public Object findFacturasSinCobro( Long cuenta) {
        validateInput(cuenta);
        Map<String, Object> respuesta = new HashMap<>();
        boolean cuentaExist = dao.cuentaExist(cuenta);
        if (!cuentaExist) {
            respuesta.put("status", 200);
            respuesta.put("message", "La cuenta: " + cuenta + " no existe.");
            return respuesta;
        }
        List<FacturasSinCobroInter> facturas = dao.findFacturasSinCobro(cuenta);
        if (facturas.isEmpty()) {
            respuesta.put("status", 200);
            respuesta.put("message", "No tiene deudas pendientes");
            return respuesta;
        }
        return buildResponse(cuenta, facturas);

    }
    public List<Map<String, Object>> findHistorialConsumo(Long idabonado, int meses) {
        List<Object[]> rows = lecturasR.findHistorialByAbonado(idabonado, meses);
        List<Map<String, Object>> result = new ArrayList<>();
        for (Object[] row : rows) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("idfactura", row[0]);
            item.put("periodo", row[1]);
            item.put("lecturaAnterior", row[2]);
            item.put("lecturaActual", row[3]);
            item.put("consumo", row[4]);
            item.put("categoria", row[5]);
            item.put("pagado", row[6]);
            item.put("fechacobro", row[7]);
            item.put("formapago", row[8]);
            item.put("totaltarifa", row[9]);
            item.put("modulo", row[10]);
            item.put("formaPagoDesc", row[11]);
            result.add(item);
        }
        return result;
    }

    public Map<String, Object> findFacturaDetalle(Long idfactura) {
        Map<String, Object> result = new LinkedHashMap<>();

        // 1. Datos base: modulo, categoria, totaltarifa, formapago
        List<Object[]> baseRows = dao.findFacturaDetail(idfactura);
        if (!baseRows.isEmpty()) {
            Object[] row = baseRows.get(0);
            result.put("idfactura", row[0]);
            result.put("idabonado", row[1]);
            result.put("idmodulo", row[2]);
            result.put("modulo", row[3]);
            result.put("idcategoria", row[4]);
            result.put("categoria", row[5]);
            result.put("totaltarifa", row[7]);
            result.put("formapago", row[8]);
            result.put("pagado", row[9]);
            result.put("fechacobro", row[10]);
            result.put("clienteNombre", row[11]);
            result.put("fechaeliminacion", row[12]);
            result.put("fechaanulacion", row[13]);
        }

        // 2. Rubros
        List<Rubroxfac> rubros = rubroxfacR.findByIdfacturaWithRubro(idfactura);
        List<Map<String, Object>> rubrosList = new ArrayList<>();
        for (Rubroxfac r : rubros) {
            Map<String, Object> rubro = new LinkedHashMap<>();
            rubro.put("idrubroxfac", r.getIdrubroxfac());
            rubro.put("cantidad", r.getCantidad());
            rubro.put("valorunitario", r.getValorunitario());
            if (r.getIdrubro_rubros() != null) {
                rubro.put("idrubro", r.getIdrubro_rubros().getIdrubro());
                rubro.put("descripcion", r.getIdrubro_rubros().getDescripcion());
            }
            rubro.put("total", r.getValorunitario() != null && r.getCantidad() != null
                    ? r.getValorunitario().multiply(BigDecimal.valueOf(r.getCantidad()))
                    : BigDecimal.ZERO);
            rubrosList.add(rubro);
        }
        result.put("rubros", rubrosList);

        // 3. Lectura + novedad
        List<Lecturas> lecturas = lecturasR.findByIdfacturaWithNovedad(idfactura);
        if (!lecturas.isEmpty()) {
            Lecturas l = lecturas.get(0);
            Map<String, Object> lecturaInfo = new LinkedHashMap<>();
            lecturaInfo.put("lecturaanterior", l.getLecturaanterior());
            lecturaInfo.put("lecturaactual", l.getLecturaactual());
            lecturaInfo.put("consumo", l.getLecturaactual() != null && l.getLecturaanterior() != null
                    ? l.getLecturaactual() - l.getLecturaanterior() : 0);
            lecturaInfo.put("fechalectura", l.getFechalectura());
            lecturaInfo.put("observaciones", l.getObservaciones());
            if (l.getIdnovedad_novedades() != null) {
                lecturaInfo.put("novedad", l.getIdnovedad_novedades().getDescripcion());
                lecturaInfo.put("idnovedad", l.getIdnovedad_novedades().getIdnovedad());
            }
            result.put("lectura", lecturaInfo);
        }

        return result;
    }

    public List<Map<String, Object>> findPagoHistorial(Long idabonado) {
        List<Object[]> rows = dao.findPagoHistorialByAbonado(idabonado);
        List<Map<String, Object>> result = new ArrayList<>();
        for (Object[] row : rows) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("idfactura", row[0]);
            item.put("totaltarifa", row[1]);
            item.put("fechacobro", row[2]);
            item.put("formapago", row[3]);
            item.put("modulo", row[4]);
            item.put("formaPagoDesc", row[5]);
            result.add(item);
        }
        return result;
    }

        /*
        * ===============================================================
        * HELPERS
        * ===============================================================
        * */

    private FacturaDTO buildResponse(Long cuenta, List<FacturasSinCobroInter> facturas) {
        BigDecimal interes = BigDecimal.ZERO;
        if (facturas == null || facturas.isEmpty()) {
            return createEmptyResponse(cuenta);
        }
        for(FacturasSinCobroInter f: facturas){
            interes = interes.add(f.getInteres() != null ? f.getInteres() : BigDecimal.ZERO);
        }

        BigDecimal subtotal = calculateSubtotal(facturas);
        List<Long> facturaIds = extractFacturaIds(facturas);

        return FacturaDTO.builder()
                .cuenta(cuenta)
                .responsablepago(facturas.get(0).getNombre())
                .total(subtotal.add(interes))
                .facturas(facturaIds)
                .cedula(facturas.get(0).getCedula())
                .direccion(facturas.get(0).getDireccion())
                .build();
    }
    private FacturaDTO createEmptyResponse(Long cuenta) {
        return FacturaDTO.builder()
                .cuenta(cuenta)
                .total(BigDecimal.ZERO)
                .facturas(Collections.emptyList())
                .build();
    }
    private BigDecimal calculateSubtotal(List<FacturasSinCobroInter> facturas) {
        return facturas.stream()
                .map(FacturasSinCobroInter::getSubtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private List<Long> extractFacturaIds(List<FacturasSinCobroInter> facturas) {
        return facturas.stream()
                .map(FacturasSinCobroInter::getIdfactura)
                .collect(Collectors.toList());
    }


}
