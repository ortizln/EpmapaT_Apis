package com.erp.sri_files.service;

import com.erp.sri_files.domain.documento.DocumentoElectronico;
import com.erp.sri_files.domain.documento.TipoDocumento;
import com.erp.sri_files.exceptions.DocumentoRecepcionException;
import com.erp.sri_files.models.Factura;
import com.erp.sri_files.models.FacturaDetalle;
import com.erp.sri_files.models.FacturaDetalleImpuesto;
import com.erp.sri_files.models.FacturaPago;
import com.erp.sri_files.services.FacturaXmlGeneratorService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Service
public class DocumentoXmlService {

    private final ObjectMapper objectMapper;
    private final FacturaXmlGeneratorService facturaXmlGeneratorService;
    private final com.erp.sri_files.validation.SriRetencionValidationService sriRetencionValidationService;

    public DocumentoXmlService(
            ObjectMapper objectMapper,
            FacturaXmlGeneratorService facturaXmlGeneratorService,
            com.erp.sri_files.validation.SriRetencionValidationService sriRetencionValidationService
    ) {
        this.objectMapper = objectMapper;
        this.facturaXmlGeneratorService = facturaXmlGeneratorService;
        this.sriRetencionValidationService = sriRetencionValidationService;
    }

    public String generar(DocumentoElectronico documento) {
        try {
            if (documento.getTipoDocumento() == TipoDocumento.FACTURA) {
                return facturaXmlGeneratorService.generarXmlFactura(mapearFactura(documento));
            }
            if (documento.getTipoDocumento() == TipoDocumento.NOTA_CREDITO) {
                return generarNotaCredito(documento);
            }
            if (documento.getTipoDocumento() == TipoDocumento.NOTA_DEBITO) {
                return generarNotaDebito(documento);
            }
            if (documento.getTipoDocumento() == TipoDocumento.GUIA_REMISION) {
                return generarGuiaRemision(documento);
            }
            if (documento.getTipoDocumento() == TipoDocumento.RETENCION) {
                return generarRetencion(documento);
            }
            throw new DocumentoRecepcionException(
                    "La generacion XML real para " + documento.getTipoDocumento().name() + " aun no esta implementada"
            );
        } catch (DocumentoRecepcionException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new DocumentoRecepcionException("No se pudo generar el XML del documento");
        }
    }

    private String generarNotaCredito(DocumentoElectronico documento) throws Exception {
        JsonNode root = objectMapper.readTree(documento.getJsonOriginal());
        String claveAcceso = ensureClaveAcceso(documento);
        String ruc = safe(documento.getEmpresa().getRuc(), "9999999999999");
        String razonSocial = escape(readText(root, "/emisor/razonSocial", null, documento.getEmpresa().getRazonSocial()));
        String nombreComercial = escape(readText(root, "/emisor/nombreComercial", null, documento.getEmpresa().getNombreComercial()));
        String dirMatriz = escape(readText(root, "/emisor/direccionMatriz", null, documento.getEmpresa().getDireccionMatriz()));
        String fechaEmision = documento.getFechaEmision().format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));
        String motivo = escape(readText(root, "/documento/motivo", null, "Ajuste comercial"));
        String numDocModificado = escape(readText(root, "/documento/numeroDocumentoModificado", documento.getNumeroDocumento(), documento.getNumeroDocumento()));
        String fechaDocModificado = escape(readText(root, "/documento/fechaEmisionDocumentoModificado", null, fechaEmision));
        String identificacion = escape(readText(root, "/receptor/identificacion", documento.getIdentificacionReceptor(), "9999999999999"));
        String razonSocialComprador = escape(readText(root, "/receptor/razonSocial", documento.getRazonSocialReceptor(), "Consumidor Final"));
        String moneda = escape(safe(documento.getMoneda(), "DOLAR"));
        BigDecimal subtotal = scaled(documento.getSubtotal() == null ? documento.getTotal() : documento.getSubtotal());
        BigDecimal impuestos = scaled(documento.getImpuestos());
        BigDecimal total = scaled(documento.getTotal());
        String codigoPorcentaje = impuestos.compareTo(BigDecimal.ZERO) > 0 ? "2" : "0";
        String detallesXml = buildDetallesNotaCredito(root.path("detalles"), documento, motivo, codigoPorcentaje, subtotal, impuestos, total);

        return """
                <?xml version="1.0" encoding="UTF-8"?>
                <notaCredito id="comprobante" version="1.0.0">
                  <infoTributaria>
                    <ambiente>%s</ambiente>
                    <tipoEmision>1</tipoEmision>
                    <razonSocial>%s</razonSocial>
                    <nombreComercial>%s</nombreComercial>
                    <ruc>%s</ruc>
                    <claveAcceso>%s</claveAcceso>
                    <codDoc>04</codDoc>
                    <estab>%s</estab>
                    <ptoEmi>%s</ptoEmi>
                    <secuencial>%s</secuencial>
                    <dirMatriz>%s</dirMatriz>
                  </infoTributaria>
                  <infoNotaCredito>
                    <fechaEmision>%s</fechaEmision>
                    <dirEstablecimiento>%s</dirEstablecimiento>
                    <tipoIdentificacionComprador>05</tipoIdentificacionComprador>
                    <razonSocialComprador>%s</razonSocialComprador>
                    <identificacionComprador>%s</identificacionComprador>
                    <obligadoContabilidad>NO</obligadoContabilidad>
                    <codDocModificado>01</codDocModificado>
                    <numDocModificado>%s</numDocModificado>
                    <fechaEmisionDocSustento>%s</fechaEmisionDocSustento>
                    <totalSinImpuestos>%s</totalSinImpuestos>
                    <valorModificacion>%s</valorModificacion>
                    <moneda>%s</moneda>
                    <totalConImpuestos>
                      <totalImpuesto>
                        <codigo>2</codigo>
                        <codigoPorcentaje>%s</codigoPorcentaje>
                        <baseImponible>%s</baseImponible>
                        <valor>%s</valor>
                      </totalImpuesto>
                    </totalConImpuestos>
                    <motivo>%s</motivo>
                  </infoNotaCredito>
                  <detalles>
                %s
                  </detalles>
                </notaCredito>
                """.formatted(
                documento.getAmbiente(),
                razonSocial,
                nombreComercial,
                ruc,
                claveAcceso,
                safe(documento.getEstablecimiento(), "001"),
                safe(documento.getPuntoEmision(), "001"),
                safe(documento.getSecuencial(), "000000001"),
                dirMatriz,
                fechaEmision,
                escape(readText(root, "/emisor/direccionEstablecimiento", null, "NA")),
                razonSocialComprador,
                identificacion,
                numDocModificado,
                fechaDocModificado,
                subtotal.toPlainString(),
                total.toPlainString(),
                moneda,
                codigoPorcentaje,
                subtotal.toPlainString(),
                impuestos.toPlainString(),
                motivo,
                detallesXml
        );
    }

    private String generarGuiaRemision(DocumentoElectronico documento) throws Exception {
        JsonNode root = objectMapper.readTree(documento.getJsonOriginal());
        String claveAcceso = ensureClaveAcceso(documento);
        String ruc = safe(documento.getEmpresa().getRuc(), "9999999999999");
        String razonSocial = escape(readText(root, "/emisor/razonSocial", null, documento.getEmpresa().getRazonSocial()));
        String nombreComercial = escape(readText(root, "/emisor/nombreComercial", null, documento.getEmpresa().getNombreComercial()));
        String dirMatriz = escape(readText(root, "/emisor/direccionMatriz", null, documento.getEmpresa().getDireccionMatriz()));
        String fechaEmision = documento.getFechaEmision().format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));
        String dirPartida = escape(readText(root, "/documento/direccionPartida", null, "NA"));
        String razonSocialTransportista = escape(readText(root, "/documento/razonSocialTransportista", null, razonSocial));
        String tipoIdTransportista = escape(readText(root, "/documento/tipoIdentificacionTransportista", null, "04"));
        String rucTransportista = escape(readText(root, "/documento/rucTransportista", null, ruc));
        String rise = escape(readText(root, "/documento/rise", null, ""));
        String obligadoContabilidad = escape(readText(root, "/documento/obligadoContabilidad", null, "NO"));
        String fechaInicio = escape(readText(root, "/documento/fechaInicioTransporte", null, fechaEmision));
        String fechaFin = escape(readText(root, "/documento/fechaFinTransporte", null, fechaEmision));
        String placa = escape(readText(root, "/documento/placa", null, "AAA0001"));
        String destinatariosXml = buildDestinatariosGuiaRemision(root, documento, claveAcceso, fechaEmision);

        return """
                <?xml version="1.0" encoding="UTF-8"?>
                <guiaRemision id="comprobante" version="1.1.0">
                  <infoTributaria>
                    <ambiente>%s</ambiente>
                    <tipoEmision>1</tipoEmision>
                    <razonSocial>%s</razonSocial>
                    <nombreComercial>%s</nombreComercial>
                    <ruc>%s</ruc>
                    <claveAcceso>%s</claveAcceso>
                    <codDoc>06</codDoc>
                    <estab>%s</estab>
                    <ptoEmi>%s</ptoEmi>
                    <secuencial>%s</secuencial>
                    <dirMatriz>%s</dirMatriz>
                  </infoTributaria>
                  <infoGuiaRemision>
                    <dirEstablecimiento>%s</dirEstablecimiento>
                    <dirPartida>%s</dirPartida>
                    <razonSocialTransportista>%s</razonSocialTransportista>
                    <tipoIdentificacionTransportista>%s</tipoIdentificacionTransportista>
                    <rucTransportista>%s</rucTransportista>
                    <rise>%s</rise>
                    <obligadoContabilidad>%s</obligadoContabilidad>
                    <contribuyenteEspecial></contribuyenteEspecial>
                    <fechaIniTransporte>%s</fechaIniTransporte>
                    <fechaFinTransporte>%s</fechaFinTransporte>
                    <placa>%s</placa>
                  </infoGuiaRemision>
                  <destinatarios>
                %s
                  </destinatarios>
                </guiaRemision>
                """.formatted(
                documento.getAmbiente(),
                razonSocial,
                nombreComercial,
                ruc,
                claveAcceso,
                safe(documento.getEstablecimiento(), "001"),
                safe(documento.getPuntoEmision(), "001"),
                safe(documento.getSecuencial(), "000000001"),
                dirMatriz,
                escape(readText(root, "/emisor/direccionEstablecimiento", null, "NA")),
                dirPartida,
                razonSocialTransportista,
                tipoIdTransportista,
                rucTransportista,
                rise,
                obligadoContabilidad,
                fechaInicio,
                fechaFin,
                placa,
                destinatariosXml
        );
    }

    private String buildDestinatariosGuiaRemision(
            JsonNode root,
            DocumentoElectronico documento,
            String claveAcceso,
            String fechaEmision
    ) {
        JsonNode destinatariosNode = root.path("destinatarios");
        if (destinatariosNode.isArray() && !destinatariosNode.isEmpty()) {
            StringBuilder xml = new StringBuilder();
            for (JsonNode destinatarioNode : destinatariosNode) {
                xml.append(buildDestinatarioGuiaRemision(
                        destinatarioNode,
                        root.path("documento"),
                        destinatarioNode.path("detalles"),
                        documento,
                        claveAcceso,
                        fechaEmision
                ));
            }
            return xml.toString();
        }

        return buildDestinatarioGuiaRemision(
                root.path("receptor"),
                root.path("documento"),
                root.path("detalles"),
                documento,
                claveAcceso,
                fechaEmision
        );
    }

    private String buildDestinatarioGuiaRemision(
            JsonNode destinatarioNode,
            JsonNode documentoNode,
            JsonNode detallesFallbackNode,
            DocumentoElectronico documento,
            String claveAcceso,
            String fechaEmision
    ) {
        String identificacionDestinatario = escape(textOrDefault(destinatarioNode.path("identificacion"), safe(documento.getIdentificacionReceptor(), "9999999999999")));
        String razonSocialDestinatario = escape(textOrDefault(destinatarioNode.path("razonSocial"), safe(documento.getRazonSocialReceptor(), "Destinatario")));
        String dirDestinatario = escape(textOrDefault(destinatarioNode.path("direccion"), "NA"));
        String motivoTraslado = escape(textOrDefault(destinatarioNode.path("motivoTraslado"), textOrDefault(documentoNode.path("motivoTraslado"), "Traslado interno")));
        String docAduanero = escape(textOrDefault(destinatarioNode.path("docAduaneroUnico"), textOrDefault(documentoNode.path("docAduaneroUnico"), "")));
        String codEstabDestino = escape(textOrDefault(destinatarioNode.path("codEstabDestino"), textOrDefault(documentoNode.path("codEstabDestino"), safe(documento.getEstablecimiento(), "001"))));
        String ruta = escape(textOrDefault(destinatarioNode.path("ruta"), textOrDefault(documentoNode.path("ruta"), "NA")));
        String codDocSustento = escape(textOrDefault(destinatarioNode.path("codDocSustento"), textOrDefault(documentoNode.path("codDocSustento"), "01")));
        String numDocSustento = escape(textOrDefault(destinatarioNode.path("numDocSustento"), textOrDefault(documentoNode.path("numDocSustento"), safe(documento.getNumeroDocumento(), "001-001-000000001"))));
        String numAutDocSustento = escape(textOrDefault(destinatarioNode.path("numAutDocSustento"), textOrDefault(documentoNode.path("numAutDocSustento"), claveAcceso)));
        String fechaEmisionDocSustento = escape(textOrDefault(destinatarioNode.path("fechaEmisionDocSustento"), textOrDefault(documentoNode.path("fechaEmisionDocSustento"), fechaEmision)));
        String detallesXml = buildDetallesDestinatarioGuia(destinatarioNode.path("detalles"), detallesFallbackNode, documento);

        return """
                    <destinatario>
                      <identificacionDestinatario>%s</identificacionDestinatario>
                      <razonSocialDestinatario>%s</razonSocialDestinatario>
                      <dirDestinatario>%s</dirDestinatario>
                      <motivoTraslado>%s</motivoTraslado>
                      <docAduaneroUnico>%s</docAduaneroUnico>
                      <codEstabDestino>%s</codEstabDestino>
                      <ruta>%s</ruta>
                      <codDocSustento>%s</codDocSustento>
                      <numDocSustento>%s</numDocSustento>
                      <numAutDocSustento>%s</numAutDocSustento>
                      <fechaEmisionDocSustento>%s</fechaEmisionDocSustento>
                      <detalles>
                %s
                      </detalles>
                    </destinatario>
                """.formatted(
                identificacionDestinatario,
                razonSocialDestinatario,
                dirDestinatario,
                motivoTraslado,
                docAduanero,
                codEstabDestino,
                ruta,
                codDocSustento,
                numDocSustento,
                numAutDocSustento,
                fechaEmisionDocSustento,
                detallesXml
        );
    }

    private String buildDetallesDestinatarioGuia(JsonNode detallesNode, JsonNode detallesFallbackNode, DocumentoElectronico documento) {
        if (detallesNode.isArray() && !detallesNode.isEmpty()) {
            StringBuilder xml = new StringBuilder();
            for (JsonNode detalleNode : detallesNode) {
                xml.append(buildDetalleGuia(detalleNode));
            }
            return xml.toString();
        }

        if (detallesFallbackNode.isArray() && !detallesFallbackNode.isEmpty()) {
            StringBuilder xml = new StringBuilder();
            for (JsonNode detalleNode : detallesFallbackNode) {
                xml.append(buildDetalleGuia(detalleNode));
            }
            return xml.toString();
        }

        JsonNode detalleFallback = objectMapper.createObjectNode()
                .put("codigo", "ITEM-1")
                .put("descripcion", safe(documento.getNumeroDocumento(), "Item trasladado"))
                .put("cantidad", "1");
        return buildDetalleGuia(detalleFallback);
    }

    private String buildDetalleGuia(JsonNode detalleNode) {
        String itemCodigo = escape(textOrDefault(detalleNode.path("codigo"), textOrDefault(detalleNode.path("codigoPrincipal"), "ITEM-1")));
        String itemCodigoAdicional = escape(textOrDefault(detalleNode.path("codigoAdicional"), textOrDefault(detalleNode.path("codigoAuxiliar"), "")));
        String itemDescripcion = escape(textOrDefault(detalleNode.path("descripcion"), "Item trasladado"));
        String itemCantidad = scaled(decimalNode(detalleNode.path("cantidad"), BigDecimal.ONE)).toPlainString();
        String codigoAdicionalXml = itemCodigoAdicional.isBlank()
                ? ""
                : "\n                  <codigoAdicional>%s</codigoAdicional>".formatted(itemCodigoAdicional);

        return """
                        <detalle>
                          <codigoInterno>%s</codigoInterno>
                %s
                          <descripcion>%s</descripcion>
                          <cantidad>%s</cantidad>
                        </detalle>
                """.formatted(itemCodigo, codigoAdicionalXml, itemDescripcion, itemCantidad);
    }

    private String generarNotaDebito(DocumentoElectronico documento) throws Exception {
        JsonNode root = objectMapper.readTree(documento.getJsonOriginal());
        String claveAcceso = ensureClaveAcceso(documento);
        String ruc = safe(documento.getEmpresa().getRuc(), "9999999999999");
        String razonSocial = escape(readText(root, "/emisor/razonSocial", null, documento.getEmpresa().getRazonSocial()));
        String nombreComercial = escape(readText(root, "/emisor/nombreComercial", null, documento.getEmpresa().getNombreComercial()));
        String dirMatriz = escape(readText(root, "/emisor/direccionMatriz", null, documento.getEmpresa().getDireccionMatriz()));
        String fechaEmision = documento.getFechaEmision().format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));
        String motivo = escape(readText(root, "/documento/motivo", null, "Ajuste comercial"));
        String numDocModificado = escape(readText(root, "/documento/numeroDocumentoModificado", documento.getNumeroDocumento(), documento.getNumeroDocumento()));
        String fechaDocModificado = escape(readText(root, "/documento/fechaEmisionDocumentoModificado", null, fechaEmision));
        String identificacion = escape(readText(root, "/receptor/identificacion", documento.getIdentificacionReceptor(), "9999999999999"));
        String razonSocialComprador = escape(readText(root, "/receptor/razonSocial", documento.getRazonSocialReceptor(), "Consumidor Final"));
        String moneda = escape(safe(documento.getMoneda(), "DOLAR"));
        BigDecimal subtotal = scaled(documento.getSubtotal() == null ? documento.getTotal() : documento.getSubtotal());
        BigDecimal impuestos = scaled(documento.getImpuestos());
        BigDecimal total = scaled(documento.getTotal());
        String codigoPorcentaje = impuestos.compareTo(BigDecimal.ZERO) > 0 ? "2" : "0";
        String motivosXml = buildMotivosNotaDebito(root.path("motivos"), motivo, total);

        return """
                <?xml version="1.0" encoding="UTF-8"?>
                <notaDebito id="comprobante" version="1.0.0">
                  <infoTributaria>
                    <ambiente>%s</ambiente>
                    <tipoEmision>1</tipoEmision>
                    <razonSocial>%s</razonSocial>
                    <nombreComercial>%s</nombreComercial>
                    <ruc>%s</ruc>
                    <claveAcceso>%s</claveAcceso>
                    <codDoc>05</codDoc>
                    <estab>%s</estab>
                    <ptoEmi>%s</ptoEmi>
                    <secuencial>%s</secuencial>
                    <dirMatriz>%s</dirMatriz>
                  </infoTributaria>
                  <infoNotaDebito>
                    <fechaEmision>%s</fechaEmision>
                    <dirEstablecimiento>%s</dirEstablecimiento>
                    <tipoIdentificacionComprador>05</tipoIdentificacionComprador>
                    <razonSocialComprador>%s</razonSocialComprador>
                    <identificacionComprador>%s</identificacionComprador>
                    <obligadoContabilidad>NO</obligadoContabilidad>
                    <codDocModificado>01</codDocModificado>
                    <numDocModificado>%s</numDocModificado>
                    <fechaEmisionDocSustento>%s</fechaEmisionDocSustento>
                    <totalSinImpuestos>%s</totalSinImpuestos>
                    <impuestos>
                      <impuesto>
                        <codigo>2</codigo>
                        <codigoPorcentaje>%s</codigoPorcentaje>
                        <tarifa>0</tarifa>
                        <baseImponible>%s</baseImponible>
                        <valor>%s</valor>
                      </impuesto>
                    </impuestos>
                    <valorTotal>%s</valorTotal>
                  </infoNotaDebito>
                  <motivos>
                %s
                  </motivos>
                </notaDebito>
                """.formatted(
                documento.getAmbiente(),
                razonSocial,
                nombreComercial,
                ruc,
                claveAcceso,
                safe(documento.getEstablecimiento(), "001"),
                safe(documento.getPuntoEmision(), "001"),
                safe(documento.getSecuencial(), "000000001"),
                dirMatriz,
                fechaEmision,
                escape(readText(root, "/emisor/direccionEstablecimiento", null, "NA")),
                razonSocialComprador,
                identificacion,
                numDocModificado,
                fechaDocModificado,
                subtotal.toPlainString(),
                codigoPorcentaje,
                subtotal.toPlainString(),
                impuestos.toPlainString(),
                total.toPlainString(),
                motivosXml
        );
    }

    private String buildDetallesNotaCredito(
            JsonNode detallesNode,
            DocumentoElectronico documento,
            String motivoFallback,
            String codigoPorcentaje,
            BigDecimal subtotalFallback,
            BigDecimal impuestosFallback,
            BigDecimal totalFallback
    ) {
        if (detallesNode.isArray() && !detallesNode.isEmpty()) {
            StringBuilder xml = new StringBuilder();
            for (JsonNode detalleNode : detallesNode) {
                xml.append(buildDetalleNotaCredito(detalleNode, codigoPorcentaje));
            }
            return xml.toString();
        }

        JsonNode detalleFallback = objectMapper.createObjectNode()
                .put("codigo", safe(documento.getNumeroDocumento(), "NC-001"))
                .put("descripcion", motivoFallback)
                .put("cantidad", "1")
                .put("precioUnitario", totalFallback.toPlainString())
                .put("descuento", "0")
                .put("precioTotalSinImpuesto", subtotalFallback.toPlainString())
                .put("baseImponible", subtotalFallback.toPlainString())
                .put("valorImpuesto", impuestosFallback.toPlainString())
                .put("codigoPorcentaje", codigoPorcentaje);
        return buildDetalleNotaCredito(detalleFallback, codigoPorcentaje);
    }

    private String buildDetalleNotaCredito(JsonNode detalleNode, String codigoPorcentajeFallback) {
        String codigoInterno = escape(textOrDefault(detalleNode.path("codigo"), textOrDefault(detalleNode.path("codigoPrincipal"), "ITEM-1")));
        String codigoAdicional = escape(textOrDefault(detalleNode.path("codigoAdicional"), textOrDefault(detalleNode.path("codigoAuxiliar"), "")));
        String descripcion = escape(textOrDefault(detalleNode.path("descripcion"), "Detalle nota credito"));
        BigDecimal cantidad = scaled(decimalNode(detalleNode.path("cantidad"), BigDecimal.ONE));
        BigDecimal precioUnitario = scaled(decimalNode(detalleNode.path("precioUnitario"), decimalNode(detalleNode.path("valorUnitario"), BigDecimal.ZERO)));
        BigDecimal descuento = scaled(decimalNode(detalleNode.path("descuento"), BigDecimal.ZERO));
        BigDecimal precioTotalSinImpuesto = scaled(decimalNode(
                detalleNode.path("precioTotalSinImpuesto"),
                cantidad.multiply(precioUnitario).subtract(descuento)
        ));
        String codigoPorcentaje = textOrDefault(detalleNode.path("codigoPorcentaje"), codigoPorcentajeFallback);
        BigDecimal baseImponible = scaled(decimalNode(detalleNode.path("baseImponible"), precioTotalSinImpuesto));
        BigDecimal valorImpuesto = scaled(decimalNode(detalleNode.path("valorImpuesto"), decimalNode(detalleNode.path("valor"), BigDecimal.ZERO)));
        String codigoAdicionalXml = codigoAdicional.isBlank()
                ? ""
                : "\n                      <codigoAdicional>%s</codigoAdicional>".formatted(codigoAdicional);

        return """
                    <detalle>
                      <codigoInterno>%s</codigoInterno>
                %s
                      <descripcion>%s</descripcion>
                      <cantidad>%s</cantidad>
                      <precioUnitario>%s</precioUnitario>
                      <descuento>%s</descuento>
                      <precioTotalSinImpuesto>%s</precioTotalSinImpuesto>
                      <impuestos>
                        <impuesto>
                          <codigo>2</codigo>
                          <codigoPorcentaje>%s</codigoPorcentaje>
                          <tarifa>0</tarifa>
                          <baseImponible>%s</baseImponible>
                          <valor>%s</valor>
                        </impuesto>
                      </impuestos>
                    </detalle>
                """.formatted(
                codigoInterno,
                codigoAdicionalXml,
                descripcion,
                cantidad.toPlainString(),
                precioUnitario.toPlainString(),
                descuento.toPlainString(),
                precioTotalSinImpuesto.toPlainString(),
                escape(codigoPorcentaje),
                baseImponible.toPlainString(),
                valorImpuesto.toPlainString()
        );
    }

    private String buildMotivosNotaDebito(JsonNode motivosNode, String motivoFallback, BigDecimal totalFallback) {
        if (motivosNode.isArray() && !motivosNode.isEmpty()) {
            StringBuilder xml = new StringBuilder();
            for (JsonNode motivoNode : motivosNode) {
                xml.append(buildMotivoNotaDebito(motivoNode));
            }
            return xml.toString();
        }

        JsonNode motivoNode = objectMapper.createObjectNode()
                .put("razon", motivoFallback)
                .put("valor", totalFallback.toPlainString());
        return buildMotivoNotaDebito(motivoNode);
    }

    private String buildMotivoNotaDebito(JsonNode motivoNode) {
        String razon = escape(textOrDefault(motivoNode.path("razon"), textOrDefault(motivoNode.path("motivo"), "Ajuste comercial")));
        BigDecimal valor = scaled(decimalNode(motivoNode.path("valor"), BigDecimal.ZERO));

        return """
                    <motivo>
                      <razon>%s</razon>
                      <valor>%s</valor>
                    </motivo>
                """.formatted(razon, valor.toPlainString());
    }

    private String generarRetencion(DocumentoElectronico documento) throws Exception {
        JsonNode root = objectMapper.readTree(documento.getJsonOriginal());
        String xml = readText(root, "/documento/xml", null, null);
        if (xml == null) {
            xml = readText(root, "/documento/xmlPlano", null, null);
        }
        if (xml == null) {
            xml = readText(root, "/documento/xmlRetencion", null, null);
        }
        if (xml == null || xml.isBlank()) {
            throw new DocumentoRecepcionException("No se encontro XML de retencion en el payload");
        }

        var validation = sriRetencionValidationService.validate(xml);
        if (!validation.valid()) {
            throw new DocumentoRecepcionException("XML de retencion invalido: " + String.join(" | ", validation.errors()));
        }
        if (documento.getClaveAcceso() == null || documento.getClaveAcceso().isBlank()) {
            documento.setClaveAcceso(validation.claveAcceso());
        }
        return xml;
    }

    private Factura mapearFactura(DocumentoElectronico documento) throws Exception {
        JsonNode root = objectMapper.readTree(documento.getJsonOriginal());

        Factura factura = new Factura();
        factura.setClaveacceso(ensureClaveAcceso(documento));
        factura.setSecuencial(safe(documento.getSecuencial(), "000000001"));
        factura.setEstablecimiento(safe(documento.getEstablecimiento(), "001"));
        factura.setPuntoemision(safe(documento.getPuntoEmision(), "001"));
        factura.setDireccionestablecimiento(readText(root, "/emisor/direccionEstablecimiento", null, "NA"));
        factura.setFechaemision(resolveFechaEmision(documento));
        factura.setTipoidentificacioncomprador(readText(root, "/receptor/tipoIdentificacion", null, "05"));
        factura.setIdentificacioncomprador(readText(root, "/receptor/identificacion", documento.getIdentificacionReceptor(), "9999999999999"));
        factura.setRazonsocialcomprador(readText(root, "/receptor/razonSocial", documento.getRazonSocialReceptor(), "Consumidor Final"));
        factura.setTelefonocomprador(readText(root, "/receptor/telefono", null, null));
        factura.setEmailcomprador(readText(root, "/receptor/email", documento.getEmailReceptor(), null));
        factura.setConcepto(readText(root, "/informacionAdicional/concepto", null, documento.getTipoDocumento().name()));
        factura.setRecaudador(readText(root, "/informacionAdicional/recaudador", null, "API"));
        factura.setReferencia(readText(root, "/informacionAdicional/referencia", documento.getExternalId(), documento.getUuid().toString()));
        factura.setDireccioncomprador(readText(root, "/receptor/direccion", null, "NA"));
        factura.setEstado("N");
        factura.setDetalles(mapearDetalles(root, factura, documento));
        factura.setPagos(mapearPagos(root, factura, documento));
        return factura;
    }

    private List<FacturaDetalle> mapearDetalles(JsonNode root, Factura factura, DocumentoElectronico documento) {
        JsonNode detallesNode = root.path("detalles");
        List<FacturaDetalle> detalles = new ArrayList<>();

        if (detallesNode.isArray() && !detallesNode.isEmpty()) {
            for (JsonNode detalleNode : detallesNode) {
                detalles.add(mapearDetalle(factura, detalleNode));
            }
        }

        if (detalles.isEmpty()) {
            FacturaDetalle detalle = new FacturaDetalle();
            detalle.setFactura(factura);
            detalle.setCodigoprincipal(safe(documento.getNumeroDocumento(), "ITEM-1"));
            detalle.setDescripcion(readText(root, "/documento/descripcion", null, documento.getTipoDocumento().name()));
            detalle.setCantidad(BigDecimal.ONE.setScale(2, RoundingMode.HALF_UP));
            detalle.setPreciounitario(scaled(documento.getSubtotal() == null ? documento.getTotal() : documento.getSubtotal()));
            detalle.setDescuento(BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP));
            detalle.setImpuestos(List.of(mapearImpuesto(detalle, "2", "0", scaled(documento.getSubtotal()), BigDecimal.ZERO)));
            detalles.add(detalle);
        }

        return detalles;
    }

    private FacturaDetalle mapearDetalle(Factura factura, JsonNode detalleNode) {
        FacturaDetalle detalle = new FacturaDetalle();
        detalle.setFactura(factura);
        detalle.setCodigoprincipal(textOrDefault(detalleNode.path("codigo"), textOrDefault(detalleNode.path("codigoPrincipal"), "ITEM-1")));
        detalle.setDescripcion(textOrDefault(detalleNode.path("descripcion"), "ITEM"));
        detalle.setCantidad(scaled(decimalNode(detalleNode.path("cantidad"), BigDecimal.ONE)));
        detalle.setPreciounitario(scaled(decimalNode(detalleNode.path("precioUnitario"), decimalNode(detalleNode.path("valorUnitario"), BigDecimal.ZERO))));
        detalle.setDescuento(scaled(decimalNode(detalleNode.path("descuento"), BigDecimal.ZERO)));

        BigDecimal base = scaled(
                detalle.getCantidad()
                        .multiply(detalle.getPreciounitario())
                        .subtract(detalle.getDescuento() == null ? BigDecimal.ZERO : detalle.getDescuento())
        );

        List<FacturaDetalleImpuesto> impuestos = new ArrayList<>();
        JsonNode impuestosNode = detalleNode.path("impuestos");
        if (impuestosNode.isArray() && !impuestosNode.isEmpty()) {
            for (JsonNode impuestoNode : impuestosNode) {
                String codigo = textOrDefault(impuestoNode.path("codigo"), "2");
                String codigoPorcentaje = textOrDefault(impuestoNode.path("codigoPorcentaje"), "0");
                BigDecimal baseImponible = scaled(decimalNode(impuestoNode.path("baseImponible"), base));
                impuestos.add(mapearImpuesto(detalle, codigo, codigoPorcentaje, baseImponible, BigDecimal.ZERO));
            }
        }

        if (impuestos.isEmpty()) {
            impuestos.add(mapearImpuesto(detalle, "2", "0", base, BigDecimal.ZERO));
        }

        detalle.setImpuestos(impuestos);
        return detalle;
    }

    private FacturaDetalleImpuesto mapearImpuesto(
            FacturaDetalle detalle,
            String codigo,
            String codigoPorcentaje,
            BigDecimal baseImponible,
            BigDecimal ignored
    ) {
        FacturaDetalleImpuesto impuesto = new FacturaDetalleImpuesto();
        impuesto.setDetalle(detalle);
        impuesto.setCodigoimpuesto(codigo);
        impuesto.setCodigoporcentaje(codigoPorcentaje);
        impuesto.setBaseimponible(scaled(baseImponible));
        return impuesto;
    }

    private List<FacturaPago> mapearPagos(JsonNode root, Factura factura, DocumentoElectronico documento) {
        List<FacturaPago> pagos = new ArrayList<>();
        JsonNode pagosNode = root.path("documento").path("pagos");
        if (pagosNode.isArray() && !pagosNode.isEmpty()) {
            for (JsonNode pagoNode : pagosNode) {
                FacturaPago pago = new FacturaPago();
                pago.setFactura(factura);
                pago.setFormapago(textOrDefault(pagoNode.path("formaPago"), "20"));
                pago.setTotal(scaled(decimalNode(pagoNode.path("total"), documento.getTotal())));
                if (pagoNode.hasNonNull("plazo")) {
                    pago.setPlazo(pagoNode.path("plazo").asInt());
                }
                if (pagoNode.hasNonNull("unidadTiempo")) {
                    pago.setUnidadtiempo(pagoNode.path("unidadTiempo").asText());
                }
                pagos.add(pago);
            }
        }

        if (pagos.isEmpty()) {
            FacturaPago pago = new FacturaPago();
            pago.setFactura(factura);
            pago.setFormapago(readText(root, "/documento/formaPago", null, "20"));
            pago.setTotal(scaled(documento.getTotal()));
            pagos.add(pago);
        }

        return pagos;
    }

    private String ensureClaveAcceso(DocumentoElectronico documento) {
        if (documento.getClaveAcceso() != null && !documento.getClaveAcceso().isBlank()) {
            return documento.getClaveAcceso();
        }
        String base = documento.getFechaEmision().format(DateTimeFormatter.ofPattern("ddMMyyyy"))
                + documento.getCodigoDocumento()
                + documento.getAmbiente()
                + onlyDigits(documento.getNumeroDocumento())
                + onlyDigits(documento.getUuid().toString())
                + onlyDigits(documento.getEmpresa().getRuc());
        String digits = (base + "0000000000000000000000000000000000000000000000000").substring(0, 49);
        documento.setClaveAcceso(digits);
        return digits;
    }

    private LocalDateTime resolveFechaEmision(DocumentoElectronico documento) {
        return documento.getFechaEmision().atStartOfDay();
    }

    private String readText(JsonNode root, String pointer, String preferred, String fallback) {
        if (preferred != null && !preferred.isBlank()) {
            return preferred;
        }
        JsonNode node = root.at(pointer);
        if (!node.isMissingNode() && !node.isNull()) {
            String text = node.asText();
            if (text != null && !text.isBlank()) {
                return text.trim();
            }
        }
        return fallback;
    }

    private BigDecimal decimalNode(JsonNode node, BigDecimal fallback) {
        if (node == null || node.isMissingNode() || node.isNull()) {
            return fallback == null ? BigDecimal.ZERO : fallback;
        }
        String text = node.asText();
        if (text == null || text.isBlank()) {
            return fallback == null ? BigDecimal.ZERO : fallback;
        }
        try {
            return new BigDecimal(text.trim());
        } catch (NumberFormatException ex) {
            return fallback == null ? BigDecimal.ZERO : fallback;
        }
    }

    private String safe(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value.trim();
    }

    private String onlyDigits(String value) {
        return value == null ? "" : value.replaceAll("\\D", "");
    }

    private BigDecimal scaled(BigDecimal value) {
        if (value == null) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }
        return value.setScale(2, RoundingMode.HALF_UP);
    }

    private String textOrDefault(JsonNode node, String fallback) {
        if (node == null || node.isMissingNode() || node.isNull()) {
            return fallback;
        }
        String text = node.asText();
        return text == null || text.isBlank() ? fallback : text.trim();
    }

    private String escape(String value) {
        if (value == null) {
            return "";
        }
        return value
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&apos;");
    }
}
