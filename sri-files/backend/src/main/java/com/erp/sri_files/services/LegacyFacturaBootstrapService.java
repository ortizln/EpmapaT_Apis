package com.erp.sri_files.services;

import com.erp.sri_files.models.Definir;
import com.erp.sri_files.models.Factura;
import com.erp.sri_files.models.FacturaDetalle;
import com.erp.sri_files.models.FacturaDetalleImpuesto;
import com.erp.sri_files.models.FacturaPago;
import com.erp.sri_files.models.Facturas;
import com.erp.sri_files.repositories.DefinirR;
import com.erp.sri_files.repositories.FacturaR;
import com.erp.sri_files.repositories.FacturasR;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;

@Service
public class LegacyFacturaBootstrapService {

    private final FacturaR facturaRepository;
    private final FacturasR facturasRepository;
    private final DefinirR definirRepository;

    public LegacyFacturaBootstrapService(
            FacturaR facturaRepository,
            FacturasR facturasRepository,
            DefinirR definirRepository
    ) {
        this.facturaRepository = facturaRepository;
        this.facturasRepository = facturasRepository;
        this.definirRepository = definirRepository;
    }

    @Transactional
    public Factura crearFacturaElectronicaLocal(Long idfactura) {
        Factura existente = facturaRepository.findByIdfactura(idfactura);
        if (existente != null) {
            return existente;
        }

        Facturas origen = facturasRepository.findByIdfactura(idfactura);
        if (origen == null) {
            throw new IllegalStateException("No existe registro legacy en facturas con id " + idfactura);
        }
        if (origen.getPagado() == null || origen.getPagado() != 1) {
            throw new IllegalStateException("La factura legacy " + idfactura + " no se encuentra pagada");
        }

        Definir definir = definirRepository.findById(1L)
                .orElseThrow(() -> new IllegalStateException("No existe configuracion base en definir con id 1"));

        SerieInfo serieInfo = resolverSerie(origen, idfactura);
        BigDecimal impuesto = scaled(zeroIfNull(origen.getSwiva()));
        BigDecimal descuento = scaled(zeroIfNull(origen.getValornotacredito()));
        BigDecimal total = resolverTotal(origen, impuesto, descuento);
        BigDecimal subtotal = scaled(total.subtract(impuesto).add(descuento));
        if (subtotal.compareTo(BigDecimal.ZERO) < 0) {
            subtotal = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }

        Factura factura = new Factura();
        factura.setIdfactura(idfactura);
        factura.setSecuencial(serieInfo.secuencial());
        factura.setEstado("I");
        factura.setDireccionestablecimiento(firstNonBlank(definir.getDireccion(), definir.getDirmatriz(), "NA"));
        factura.setFechaemision(LocalDateTime.now());
        factura.setEstablecimiento(serieInfo.establecimiento());
        factura.setPuntoemision(serieInfo.puntoEmision());
        factura.setTipoidentificacioncomprador("07");
        factura.setIdentificacioncomprador("9999999999999");
        factura.setRazonsocialcomprador("CONSUMIDOR FINAL");
        factura.setConcepto(firstNonBlank(origen.getNrofactura(), "Factura legacy " + idfactura));
        factura.setRecaudador("SRI-FILES");
        factura.setReferencia(origen.getIdabonado() != null ? origen.getIdabonado().toString() : idfactura.toString());
        factura.setDireccioncomprador("NA");
        factura.setErrores("Factura bootstrap local creada automaticamente desde tabla facturas");

        FacturaDetalle detalle = new FacturaDetalle();
        detalle.setFactura(factura);
        detalle.setCodigoprincipal("SERV-LEGACY");
        detalle.setDescripcion("Factura bootstrap local " + firstNonBlank(origen.getNrofactura(), idfactura.toString()));
        detalle.setCantidad(BigDecimal.ONE.setScale(2, RoundingMode.HALF_UP));
        detalle.setPreciounitario(subtotal);
        detalle.setDescuento(descuento);

        FacturaDetalleImpuesto detalleImpuesto = new FacturaDetalleImpuesto();
        detalleImpuesto.setDetalle(detalle);
        detalleImpuesto.setCodigoimpuesto("2");
        detalleImpuesto.setCodigoporcentaje(resolveCodigoPorcentaje(definir, impuesto));
        detalleImpuesto.setBaseimponible(subtotal);
        detalle.setImpuestos(List.of(detalleImpuesto));

        FacturaPago pago = new FacturaPago();
        pago.setFactura(factura);
        pago.setFormapago(origen.getFormapago() != null ? origen.getFormapago().toString() : "20");
        pago.setTotal(total);

        factura.setDetalles(List.of(detalle));
        factura.setPagos(List.of(pago));
        return facturaRepository.save(factura);
    }

    private BigDecimal resolverTotal(Facturas origen, BigDecimal impuesto, BigDecimal descuento) {
        BigDecimal totalTarifa = zeroIfNull(origen.getTotaltarifa());
        if (totalTarifa.compareTo(BigDecimal.ZERO) > 0) {
            return scaled(totalTarifa);
        }

        BigDecimal base = zeroIfNull(origen.getValorbase());
        BigDecimal interes = zeroIfNull(origen.getInterescobrado());
        return scaled(base.add(impuesto).add(interes).subtract(descuento));
    }

    private String resolveCodigoPorcentaje(Definir definir, BigDecimal impuesto) {
        if (impuesto.compareTo(BigDecimal.ZERO) <= 0) {
            return "0";
        }

        BigDecimal porcentaje = definir.getPorciva() != null
                ? definir.getPorciva()
                : BigDecimal.valueOf(definir.getIva());
        int valor = porcentaje.setScale(0, RoundingMode.HALF_UP).intValue();
        return switch (valor) {
            case 12 -> "2";
            case 14 -> "3";
            case 15 -> "4";
            default -> "4";
        };
    }

    private SerieInfo resolverSerie(Facturas origen, Long idfactura) {
        String numero = firstNonBlank(origen.getNrofactura(), origen.getSecuencialfacilito(), "");
        String[] partes = numero.split("-");
        if (partes.length >= 3) {
            return new SerieInfo(
                    leftPadDigits(partes[0], 3, "001"),
                    leftPadDigits(partes[1], 3, "001"),
                    leftPadDigits(partes[2], 9, String.format(Locale.ROOT, "%09d", idfactura))
            );
        }

        return new SerieInfo("001", "001", String.format(Locale.ROOT, "%09d", idfactura));
    }

    private String leftPadDigits(String value, int size, String fallback) {
        String digits = value == null ? "" : value.replaceAll("\\D", "");
        if (digits.isBlank()) {
            return fallback;
        }
        if (digits.length() > size) {
            digits = digits.substring(digits.length() - size);
        }
        return String.format(Locale.ROOT, "%0" + size + "d", Long.parseLong(digits));
    }

    private String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value.trim();
            }
        }
        return null;
    }

    private BigDecimal zeroIfNull(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    private BigDecimal scaled(BigDecimal value) {
        return value.setScale(2, RoundingMode.HALF_UP);
    }

    private record SerieInfo(String establecimiento, String puntoEmision, String secuencial) {
    }
}
