package com.erp.epmapaApi.services;

import com.erp.epmapaApi.DTO.FacturaDTO;
import com.erp.epmapaApi.Interfaces.FacturasSinCobroInter;
import com.erp.epmapaApi.repositories.FacturasR;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RequiredArgsConstructor
@Service
public class FacturaService {
    private final FacturasR dao;

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
