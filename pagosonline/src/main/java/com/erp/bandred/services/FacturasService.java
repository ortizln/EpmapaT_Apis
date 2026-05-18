package com.erp.bandred.services;

import ch.qos.logback.classic.Logger;
import com.erp.bandred.DTO.FacturaDTO;
import com.erp.bandred.interfaces.FacturasCobradas;
import com.erp.bandred.interfaces.FacturasSinCobroInter;
import com.erp.bandred.interfaces.NroFactura_int;
import com.erp.bandred.models.Facturas;
import com.erp.bandred.repositories.CajasR;
import com.erp.bandred.repositories.FacturasR;
import com.erp.bandred.repositories.TmpinteresxfacR;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class FacturasService {
    private static final Logger log = (Logger) LoggerFactory.getLogger(FacturasService.class);

    private final FacturasR dao;

    @Autowired
    private TmpinteresxfacR tmpinteresxfacR;

    @Autowired
    private CajasR cajasR;

    @Autowired
    private RecaudaxcajaService rxc_service;

    @Autowired
    private CajasService cajasService;

    @Autowired
    private RestTemplate restTemplate;

    @Value("${app.fec-factura.base-url}")
    private String fecFacturaBaseUrl;

    @Autowired
    public FacturasService(FacturasR dao) {
        this.dao = dao;
    }

    public Object findFacturasSinCobro(Long user, Long cuenta) {
        validateInput(user, cuenta);
        Map<String, Object> respuesta = new HashMap<>();
        Map<String, Object> connection = (Map<String, Object>) cajasService.testIfLogin(user);
        Boolean test = validateCajaStatus(connection);
        if (test) {
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

        } else {
            return respuesta.put("mensaje", "Caja no iniciada");
        }

    }

    public List<Long> getListaPlanillas(Long cuenta) {
        return dao.findPlanillas(cuenta);
    }

    private void validateInput(Long user, Long cuenta) {
        if (user == null || cuenta == null) {
            throw new IllegalArgumentException("Los parametros 'user' y 'cuenta' no pueden ser nulos");
        }
    }

    public Optional<Facturas> findFacturaById(Long idfactura) {
        return dao.findById(idfactura);
    }

    public Boolean validateCajaStatus(Map<String, Object> connection) {
        Boolean respuesta = false;
        if (connection == null || !connection.containsKey("estado")) {
            return false;
        }
        Object estado = connection.get("estado");
        if (estado instanceof Boolean) {
            respuesta = (Boolean) estado;
        } else if (estado instanceof Integer) {
            respuesta = ((Integer) estado) == 1;
        } else if (estado instanceof String) {
            String estadoStr = ((String) estado).toLowerCase();
            if ("true".equals(estadoStr) || "1".equals(estadoStr)) {
                respuesta = true;
            }
            if ("false".equals(estadoStr) || "0".equals(estadoStr)) {
                respuesta = false;
            }
        }
        return respuesta;
    }

    private FacturaDTO buildResponse(Long cuenta, List<FacturasSinCobroInter> facturas) {
        BigDecimal interes = BigDecimal.ZERO;
        if (facturas == null || facturas.isEmpty()) {
            return createEmptyResponse(cuenta);
        }
        for (FacturasSinCobroInter f : facturas) {
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

    private FacturaDTO createEmptyResponse(Long cuenta) {
        return FacturaDTO.builder()
                .cuenta(cuenta)
                .total(BigDecimal.ZERO)
                .facturas(Collections.emptyList())
                .build();
    }

    public static class ServiceUnavailableException extends RuntimeException {
        public ServiceUnavailableException(String message) {
            super(message);
        }
    }

    public <S extends Facturas> Facturas __cobrarFactura(S factura) {
        return cobrarFactura(factura);
    }

    public <S extends Facturas> Facturas cobrarFactura(S factura) {
        if (factura.getNrofactura() == null) {
            NroFactura_int nroFactura = cajasR.buildNroFactura(factura.getUsuariocobro());
            String secuencial = fSecuencial(nroFactura.getSecuencial());
            String puntoEmision = fPemiCaja(nroFactura.getEstablecimiento());
            String codigo = fPemiCaja(nroFactura.getCodigo());
            String nrofactura = puntoEmision + '-' + codigo + '-' + secuencial;
            rxc_service.updateLastfactFinFac(factura.getUsuariocobro(), Long.valueOf(secuencial));
            factura.setNrofactura(nrofactura);
        }

        Facturas savedFactura = dao.save(factura);
        emitirFacturaElectronica(savedFactura.getIdfactura());
        return savedFactura;
    }

    private void emitirFacturaElectronica(Long idfactura) {
        String url = UriComponentsBuilder.fromHttpUrl(fecFacturaBaseUrl)
                .path("/createFacElectro")
                .queryParam("idfactura", idfactura)
                .toUriString();

        try {
            ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);
            if (response.getStatusCode().is2xxSuccessful()) {
                log.info("Factura electronica emitida correctamente para idfactura={}", idfactura);
            } else {
                log.warn("fec_facturaapi respondio con estado {} para idfactura={}", response.getStatusCode(), idfactura);
            }
        } catch (ResourceAccessException e) {
            log.warn("No se pudo conectar con fec_facturaapi en {} para idfactura={}", fecFacturaBaseUrl, idfactura);
        } catch (HttpClientErrorException | HttpServerErrorException e) {
            log.warn("fec_facturaapi devolvio error HTTP {} para idfactura={}", e.getStatusCode(), idfactura);
        } catch (Exception e) {
            log.error("Error inesperado al emitir factura electronica para idfactura={}", idfactura, e);
        }
    }

    public <S extends Facturas> Facturas _cobrarFactura(S factura) {
        return cobrarFactura(factura);
    }

    public static String fSecuencial(Long numero) {
        String formato = "%09d";
        return String.format(formato, numero);
    }

    public static String fPemiCaja(Long numero) {
        String formato = "%03d";
        return String.format(formato, numero);
    }

    public List<FacturasCobradas> getReporteFacturasCobradas(Long idusuario, LocalDate df, LocalDate hf, LocalTime dh, LocalTime hh) {
        return dao.getReporteFacturasCobradas(idusuario, df, hf, dh, hh);
    }
}
