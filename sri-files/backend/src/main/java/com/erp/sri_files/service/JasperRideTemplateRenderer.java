package com.erp.sri_files.service;

import com.erp.sri_files.domain.documento.Empresa;
import com.erp.sri_files.domain.documento.DocumentoElectronico;
import com.erp.sri_files.domain.documento.PlantillaRide;
import com.erp.sri_files.domain.documento.RecursoEmpresa;
import com.erp.sri_files.domain.documento.TipoDocumento;
import com.erp.sri_files.exceptions.DocumentoRecepcionException;
import com.erp.sri_files.dto.response.RideContratoCampoResponse;
import com.erp.sri_files.dto.response.RideContratoDocumentoResponse;
import com.erp.sri_files.dto.response.RideContratoSeccionResponse;
import com.erp.sri_files.repositories.documento.PlantillaRideRepository;
import com.erp.sri_files.repositories.documento.RecursoEmpresaRepository;
import net.sf.jasperreports.engine.*;
import net.sf.jasperreports.engine.data.JRMapCollectionDataSource;
import org.springframework.stereotype.Service;
import org.w3c.dom.*;
import org.xml.sax.InputSource;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

@Service
public class JasperRideTemplateRenderer {

    private final RecursoEmpresaRepository recursoEmpresaRepository;
    private final PlantillaRideRepository plantillaRideRepository;

    public JasperRideTemplateRenderer(
            RecursoEmpresaRepository recursoEmpresaRepository,
            PlantillaRideRepository plantillaRideRepository
    ) {
        this.recursoEmpresaRepository = recursoEmpresaRepository;
        this.plantillaRideRepository = plantillaRideRepository;
    }

    public void verificar(byte[] jrxml) {
        try (ByteArrayInputStream inputStream = new ByteArrayInputStream(jrxml)) {
            JasperCompileManager.compileReport(inputStream);
        } catch (Exception ex) {
            throw new DocumentoRecepcionException("La plantilla JRXML no es valida: " + ex.getMessage());
        }
    }

    public byte[] render(String xmlAutorizado, TipoDocumento tipoDocumento, Path jrxmlPath, Empresa empresa) {
        try {
            byte[] templateBytes = Files.readAllBytes(jrxmlPath);
            verificar(templateBytes);

            Document document = parseXml(xmlAutorizado);
            Map<String, Object> params = buildParams(document, empresa, tipoDocumento);
            Collection<Map<String, ?>> rows = buildRows(document, tipoDocumento);

            try (ByteArrayInputStream templateStream = new ByteArrayInputStream(templateBytes)) {
                JasperReport report = JasperCompileManager.compileReport(templateStream);
                JRDataSource dataSource = rows.isEmpty()
                        ? new JREmptyDataSource(1)
                        : new JRMapCollectionDataSource(rows);
                JasperPrint print = JasperFillManager.fillReport(report, params, dataSource);
                return JasperExportManager.exportReportToPdf(print);
            }
        } catch (DocumentoRecepcionException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new DocumentoRecepcionException("No fue posible generar el RIDE con la plantilla JRXML: " + ex.getMessage());
        }
    }

    private Document parseXml(String xmlAutorizado) throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(true);
        DocumentBuilder builder = factory.newDocumentBuilder();
        Document original = builder.parse(new InputSource(new java.io.StringReader(xmlAutorizado)));
        NodeList comprobantes = original.getElementsByTagName("comprobante");
        if (comprobantes.getLength() > 0) {
            String innerXml = comprobantes.item(0).getTextContent();
            return builder.parse(new InputSource(new java.io.StringReader(innerXml)));
        }
        return original;
    }

    private Map<String, Object> buildParams(Document document, Empresa empresa, TipoDocumento tipoDocumento) {
        Map<String, Object> params = new LinkedHashMap<>();
        collectLeafNodes(document.getDocumentElement(), params);
        params.put("TipoDocumento", tipoDocumento.name());
        params.put("EmpresaRuc", empresa.getRuc());
        params.put("EmpresaRazonSocial", empresa.getRazonSocial());
        params.put("EmpresaNombreComercial", empresa.getNombreComercial());
        params.put("EmpresaDireccionMatriz", empresa.getDireccionMatriz());

        List<RecursoEmpresa> recursos = recursoEmpresaRepository.findByEmpresaOrderByCreatedAtDesc(empresa);
        for (RecursoEmpresa recurso : recursos) {
            if (!recurso.isActivo()) {
                continue;
            }
            String prefix = recurso.getTipo().name();
            params.put(prefix + "_PATH", recurso.getRuta());
            params.put(prefix + "_NAME", recurso.getNombreArchivo());
        }

        alias(params, "RazonSocial", "razonSocial");
        alias(params, "Ruc", "ruc");
        alias(params, "NombreComercial", "nombreComercial");
        alias(params, "DireccionMatriz", "dirMatriz");
        alias(params, "FechaEmision", "fechaEmision");
        alias(params, "ClaveAcceso", "claveAcceso");
        alias(params, "NumeroAutorizacion", "numeroAutorizacion");
        alias(params, "FechaAutorizacion", "fechaAutorizacion");
        alias(params, "Ambiente", "ambiente");
        alias(params, "NroFactura", "numeroDocumento");
        alias(params, "NumeroDocumento", "numeroDocumento");
        params.putIfAbsent("numeroDocumento", numeroDocumento(params));
        params.putIfAbsent("NumeroDocumento", numeroDocumento(params));
        return params;
    }

    private void alias(Map<String, Object> params, String target, String source) {
        Object value = params.get(source);
        if (value != null && !String.valueOf(value).isBlank()) {
            params.put(target, value);
        }
    }

    private String numeroDocumento(Map<String, Object> params) {
        String estab = text(params.get("estab"));
        String ptoEmi = text(params.get("ptoEmi"));
        String secuencial = text(params.get("secuencial"));
        if (estab == null || ptoEmi == null || secuencial == null) {
            return null;
        }
        return estab + "-" + ptoEmi + "-" + secuencial;
    }

    private Collection<Map<String, ?>> buildRows(Document document, TipoDocumento tipoDocumento) {
        List<Map<String, ?>> rows = new ArrayList<>();
        String nodeName = switch (tipoDocumento) {
            case RETENCION -> "impuesto";
            case GUIA_REMISION -> "destinatario";
            default -> "detalle";
        };

        NodeList nodes = document.getElementsByTagName(nodeName);
        for (int i = 0; i < nodes.getLength(); i++) {
            Node node = nodes.item(i);
            if (node instanceof Element element) {
                Map<String, Object> item = new LinkedHashMap<>();
                collectLeafNodes(element, item);
                rows.add(item);
            }
        }

        if (rows.isEmpty()) {
            Map<String, Object> single = new LinkedHashMap<>();
            collectLeafNodes(document.getDocumentElement(), single);
            rows.add(single);
        }
        return rows;
    }

    private void collectLeafNodes(Element root, Map<String, Object> params) {
        collectLeafNodes(root, params, new HashSet<>());
    }

    private void collectLeafNodes(Element element, Map<String, Object> params, Set<String> seen) {
        NodeList children = element.getChildNodes();
        boolean hasElementChildren = false;
        for (int i = 0; i < children.getLength(); i++) {
            Node child = children.item(i);
            if (child instanceof Element childElement) {
                hasElementChildren = true;
                collectLeafNodes(childElement, params, seen);
            }
        }
        if (!hasElementChildren) {
            String tag = element.getTagName();
            if (seen.add(tag)) {
                String value = element.getTextContent();
                if (value != null) {
                    params.put(tag, value.trim());
                }
            }
        }
    }

    private String text(Object value) {
        if (value == null) {
            return null;
        }
        String text = String.valueOf(value).trim();
        return text.isEmpty() ? null : text;
    }

    public RideContratoDocumentoResponse construirContrato(DocumentoElectronico documento, String xmlAutorizado) {
        try {
            Document parsed = parseXml(xmlAutorizado);
            Map<String, Object> params = buildParams(parsed, documento.getEmpresa(), documento.getTipoDocumento());
            Collection<Map<String, ?>> rows = buildRows(parsed, documento.getTipoDocumento());
            PlantillaRide plantilla = documento.getEmpresa() == null ? null : plantillaRideRepository
                    .findFirstByEmpresaAndTipoDocumentoAndPredeterminadaTrueAndActivaTrue(documento.getEmpresa(), documento.getTipoDocumento())
                    .orElse(null);

            List<RideContratoCampoResponse> parametros = params.entrySet().stream()
                    .sorted(Map.Entry.comparingByKey())
                    .map(entry -> new RideContratoCampoResponse(entry.getKey(), entry.getValue() != null ? String.valueOf(entry.getValue()) : null))
                    .toList();

            Map<String, ?> sampleRow = rows.stream().findFirst().orElse(Map.of());
            List<RideContratoCampoResponse> detailFields = sampleRow.entrySet().stream()
                    .sorted(Map.Entry.comparingByKey())
                    .map(entry -> new RideContratoCampoResponse(entry.getKey(), entry.getValue() != null ? String.valueOf(entry.getValue()) : null))
                    .toList();

            List<RideContratoCampoResponse> recursos = recursoEmpresaRepository.findByEmpresaOrderByCreatedAtDesc(documento.getEmpresa()).stream()
                    .filter(RecursoEmpresa::isActivo)
                    .flatMap(recurso -> java.util.stream.Stream.of(
                            new RideContratoCampoResponse(recurso.getTipo().name() + "_PATH", recurso.getRuta()),
                            new RideContratoCampoResponse(recurso.getTipo().name() + "_NAME", recurso.getNombreArchivo())
                    ))
                    .toList();

            return new RideContratoDocumentoResponse(
                    documento.getUuid().toString(),
                    documento.getEmpresa() != null ? documento.getEmpresa().getUuid().toString() : null,
                    documento.getTipoDocumento().name(),
                    plantilla != null ? plantilla.getUuid().toString() : null,
                    parametros,
                    new RideContratoSeccionResponse(detailNodeName(documento.getTipoDocumento()), detailFields),
                    recursos
            );
        } catch (Exception ex) {
            throw ex instanceof DocumentoRecepcionException dre
                    ? dre
                    : new DocumentoRecepcionException("No fue posible construir el contrato RIDE: " + ex.getMessage());
        }
    }

    private String detailNodeName(TipoDocumento tipoDocumento) {
        return switch (tipoDocumento) {
            case RETENCION -> "impuesto";
            case GUIA_REMISION -> "destinatario";
            default -> "detalle";
        };
    }
}
