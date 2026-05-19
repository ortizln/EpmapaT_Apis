package com.erp.epmapaApi.controllers;

import com.erp.epmapaApi.dto.FacturaElectronicaDTO;
import com.erp.epmapaApi.models.Clientes;
import com.erp.epmapaApi.models.Facturas;
import com.erp.epmapaApi.models.sri.FecFactura;
import com.erp.epmapaApi.models.sri.FacturaPago;
import com.erp.epmapaApi.repositories.ClientesR;
import com.erp.epmapaApi.repositories.FacturasR;
import com.erp.epmapaApi.repositories.sri.FecFacturaR;
import com.erp.epmapaApi.services.FacturaService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/facturas")
public class FacturasApi {
    private final FacturaService facturaService;
    private final FecFacturaR fecFacturaR;
    private final ClientesR clientesR;
    private final FacturasR facturasR;

    // Módulos considerados "consumo de agua"
    private static final List<Long> MODULOS_AGUA = List.of(3L, 4L);

    @GetMapping("/sincobrar")
    public ResponseEntity<Object> getFacturasSinCobro(@RequestParam(required = false) Long cuenta,
                                                       @RequestParam(required = false) String identificacion) throws Exception {
        if (cuenta != null) {
            Object datos = facturaService.findFacturasSinCobro(cuenta);
            if (datos == null) {
                return ResponseEntity.ok(Map.of("mensaje", "factura no encontradas"));
            }
            return ResponseEntity.ok(datos);
        }
        return ResponseEntity.badRequest().body(Map.of("mensaje", "Debe proporcionar 'cuenta'"));
    }

    @GetMapping("/fac_electronicas-abo")
    public ResponseEntity<?> getFacturasElectronicasPorAbonado(
            @RequestParam Long idabonado,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        List<FecFactura> facturas = fecFacturaR.findByReferencia(String.valueOf(idabonado));
        if (facturas.isEmpty()) {
            return ResponseEntity.noContent().build();
        }
        List<FacturaElectronicaDTO> dtos = facturas.stream()
                .map(this::toDto)
                .collect(Collectors.toList());
        enriquecerConModulo(dtos);
        List<FacturaElectronicaDTO> paginated = paginateList(dtos, page, size);
        Map<String, Object> response = buildPageResponse(paginated, dtos.size(), page, size);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/fac_electronicas-cli")
    public ResponseEntity<?> getFacturasElectronicasPorCliente(
            @RequestParam Long idcliente,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        String cedula = clientesR.findById(idcliente)
                .map(Clientes::getCedula)
                .orElse(null);
        if (cedula == null) {
            return ResponseEntity.badRequest().body(Map.of("mensaje", "Cliente no encontrado con id: " + idcliente));
        }
        List<FecFactura> facturas = fecFacturaR.findByIdentificacioncomprador(cedula);
        if (facturas.isEmpty()) {
            return ResponseEntity.noContent().build();
        }
        List<FacturaElectronicaDTO> dtos = facturas.stream()
                .map(this::toDto)
                .collect(Collectors.toList());
        enriquecerConModulo(dtos);
        List<FacturaElectronicaDTO> paginated = paginateList(dtos, page, size);
        Map<String, Object> response = buildPageResponse(paginated, dtos.size(), page, size);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/fac_electronicas-servicio")
    public ResponseEntity<?> getFacturasElectronicasServicio(
            @RequestParam Long idcliente,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        String cedula = clientesR.findById(idcliente)
                .map(Clientes::getCedula)
                .orElse(null);
        if (cedula == null) {
            return ResponseEntity.badRequest().body(Map.of("mensaje", "Cliente no encontrado con id: " + idcliente));
        }
        List<FecFactura> facturas = fecFacturaR.findByIdentificacioncomprador(cedula);
        if (facturas.isEmpty()) {
            return ResponseEntity.noContent().build();
        }
        List<FacturaElectronicaDTO> dtos = facturas.stream()
                .map(this::toDto)
                .collect(Collectors.toList());
        enriquecerConModulo(dtos);
        List<FacturaElectronicaDTO> soloServicio = dtos.stream()
                .filter(d -> d.getIdmodulo() == null || !MODULOS_AGUA.contains(d.getIdmodulo()))
                .collect(Collectors.toList());
        List<FacturaElectronicaDTO> paginated = paginateList(soloServicio, page, size);
        Map<String, Object> response = buildPageResponse(paginated, soloServicio.size(), page, size);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/historial-consumo")
    public ResponseEntity<?> getHistorialConsumo(@RequestParam Long idabonado,
                                                  @RequestParam(defaultValue = "12") int meses) {
        List<Map<String, Object>> historial = facturaService.findHistorialConsumo(idabonado, meses);
        if (historial.isEmpty()) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.ok(historial);
    }

    @GetMapping("/detalle-completo")
    public ResponseEntity<?> getFacturaDetalle(@RequestParam Long idfactura) {
        Map<String, Object> detalle = facturaService.findFacturaDetalle(idfactura);
        if (detalle.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(detalle);
    }

    @GetMapping("/historial-pago")
    public ResponseEntity<?> getPagoHistorial(@RequestParam Long idabonado) {
        List<Map<String, Object>> historial = facturaService.findPagoHistorial(idabonado);
        if (historial.isEmpty()) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.ok(historial);
    }

    private void enriquecerConModulo(List<FacturaElectronicaDTO> dtos) {
        List<Long> ids = dtos.stream()
                .map(FacturaElectronicaDTO::getIdfactura)
                .collect(Collectors.toList());
        if (ids.isEmpty()) return;
        List<Facturas> erpFacturas = facturasR.findByIdfacturaInWithModulo(ids);
        Map<Long, Long> moduloMap = erpFacturas.stream()
                .filter(f -> f.getIdmodulo() != null)
                .collect(Collectors.toMap(
                        Facturas::getIdfactura,
                        f -> f.getIdmodulo().getIdmodulo(),
                        (a, b) -> a));
        for (FacturaElectronicaDTO dto : dtos) {
            dto.setIdmodulo(moduloMap.get(dto.getIdfactura()));
        }
    }

    private List<FacturaElectronicaDTO> paginateList(List<FacturaElectronicaDTO> list, int page, int size) {
        int start = page * size;
        if (start >= list.size()) return List.of();
        int end = Math.min(start + size, list.size());
        return list.subList(start, end);
    }

    private Map<String, Object> buildPageResponse(List<?> content, long totalElements, int page, int size) {
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("content", content);
        response.put("totalElements", totalElements);
        response.put("totalPages", (int) Math.ceil((double) totalElements / size));
        response.put("number", page);
        response.put("size", size);
        return response;
    }

    private FacturaElectronicaDTO toDto(FecFactura f) {
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
