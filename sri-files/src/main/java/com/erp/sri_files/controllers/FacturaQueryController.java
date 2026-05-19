package com.erp.sri_files.controllers;

import com.erp.sri_files.dto.FacturaElectronicaDTO;
import com.erp.sri_files.models.Factura;
import com.erp.sri_files.models.FacturaPago;
import com.erp.sri_files.repositories.FacturaR;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/singsend")
public class FacturaQueryController {

    private final FacturaR facturaR;

    @GetMapping("/facturas-por-abonado")
    public ResponseEntity<List<FacturaElectronicaDTO>> getFacturasPorAbonado(@RequestParam String idabonado) {
        List<Factura> facturas = facturaR.findByReferencia(idabonado);
        if (facturas.isEmpty()) {
            return ResponseEntity.noContent().build();
        }
        List<FacturaElectronicaDTO> dtos = facturas.stream()
                .map(this::toDto)
                .collect(Collectors.toList());
        return ResponseEntity.ok(dtos);
    }

    @GetMapping("/facturas-por-cliente-cedula")
    public ResponseEntity<List<FacturaElectronicaDTO>> getFacturasPorClienteCedula(@RequestParam String cedula) {
        List<Factura> facturas = facturaR.findByIdentificacioncomprador(cedula);
        if (facturas.isEmpty()) {
            return ResponseEntity.noContent().build();
        }
        List<FacturaElectronicaDTO> dtos = facturas.stream()
                .map(this::toDto)
                .collect(Collectors.toList());
        return ResponseEntity.ok(dtos);
    }

    private FacturaElectronicaDTO toDto(Factura f) {
        BigDecimal total = f.getPagos() != null ?
                f.getPagos().stream()
                        .map(FacturaPago::getTotal)
                        .reduce(BigDecimal.ZERO, BigDecimal::add) :
                BigDecimal.ZERO;
        String nrofactura = String.format("%s-%s-%s",
                f.getEstablecimiento(), f.getPuntoemision(), f.getSecuencial());
        return FacturaElectronicaDTO.builder()
                .idfactura(f.getIdfactura())
                .nrofactura(nrofactura)
                .razonsocialcomprador(f.getRazonsocialcomprador())
                .emailcomprador(f.getEmailcomprador())
                .fechaemision(f.getFechaemision())
                .estado(f.getEstado())
                .xmlautorizado(f.getXmlautorizado())
                .referencia(f.getReferencia())
                .direccioncomprador(f.getDireccioncomprador())
                .total(total)
                .build();
    }
}
