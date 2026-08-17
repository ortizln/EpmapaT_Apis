package com.erp.sri_files.service;

import com.erp.sri_files.domain.documento.DocumentoElectronico;
import com.erp.sri_files.domain.documento.DocumentoEstado;
import com.erp.sri_files.domain.documento.DocumentoEstadoHistorial;
import com.erp.sri_files.domain.documento.Empresa;
import com.erp.sri_files.domain.documento.TipoDocumento;
import com.erp.sri_files.dto.request.DocumentoRecepcionRequest;
import com.erp.sri_files.dto.response.DocumentoRecepcionResponse;
import com.erp.sri_files.exceptions.DocumentoRecepcionException;
import com.erp.sri_files.repositories.documento.DocumentoElectronicoRepository;
import com.erp.sri_files.repositories.documento.DocumentoEstadoHistorialRepository;
import com.erp.sri_files.repositories.documento.EmpresaRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Service
public class DocumentoRecepcionService {

    private final DocumentoElectronicoRepository documentoElectronicoRepository;
    private final DocumentoEstadoHistorialRepository historialRepository;
    private final EmpresaRepository empresaRepository;
    private final ObjectMapper objectMapper;
    private final ArchivoDocumentoService archivoDocumentoService;

    public DocumentoRecepcionService(
            DocumentoElectronicoRepository documentoElectronicoRepository,
            DocumentoEstadoHistorialRepository historialRepository,
            EmpresaRepository empresaRepository,
            ObjectMapper objectMapper,
            ArchivoDocumentoService archivoDocumentoService
    ) {
        this.documentoElectronicoRepository = documentoElectronicoRepository;
        this.historialRepository = historialRepository;
        this.empresaRepository = empresaRepository;
        this.objectMapper = objectMapper;
        this.archivoDocumentoService = archivoDocumentoService;
    }

    @Transactional
    public DocumentoRecepcionResponse recibir(DocumentoRecepcionRequest request, String idempotencyKey) {
        validarContratoBase(request);
        TipoDocumento tipoDocumento = parseTipoDocumento(request.tipoDocumento());
        Empresa empresa = resolverEmpresa(request);

        Optional<DocumentoElectronico> duplicado = buscarDuplicado(empresa.getId(), request.externalId(), idempotencyKey);
        if (duplicado.isPresent()) {
            DocumentoElectronico existente = duplicado.get();
            return new DocumentoRecepcionResponse(
                    existente.getUuid().toString(),
                    existente.getTipoDocumento().name(),
                    existente.getEstadoActual().name(),
                    "Documento ya recibido anteriormente",
                    true
            );
        }

        DocumentoElectronico documento = new DocumentoElectronico();
        documento.setUuid(UUID.randomUUID());
        documento.setEmpresa(empresa);
        documento.setTipoDocumento(tipoDocumento);
        documento.setAmbiente(resolveAmbiente(request, empresa));
        documento.setEstadoActual(DocumentoEstado.RECIBIDO);
        documento.setExternalId(blankToNull(request.externalId()));
        documento.setIdempotencyKey(blankToNull(idempotencyKey));
        documento.setCodigoDocumento(tipoDocumento.getCodigoSri());
        documento.setEstablecimiento(extractText(request.emisor(), "establecimiento"));
        documento.setPuntoEmision(extractText(request.emisor(), "puntoEmision"));
        documento.setSecuencial(extractText(request.documento(), "secuencial"));
        documento.setNumeroDocumento(buildNumeroDocumento(documento.getEstablecimiento(), documento.getPuntoEmision(), documento.getSecuencial()));
        documento.setClaveAcceso(extractText(request.documento(), "claveAcceso"));
        documento.setFechaEmision(resolveFechaEmision(request));
        documento.setIdentificacionReceptor(extractText(request.receptor(), "identificacion"));
        documento.setRazonSocialReceptor(extractText(request.receptor(), "razonSocial"));
        documento.setEmailReceptor(resolveEmail(request));
        documento.setMoneda(extractText(request.documento(), "moneda"));
        documento.setSubtotal(extractDecimal(request.documento(), "subtotal"));
        documento.setDescuento(extractDecimal(request.documento(), "descuento"));
        documento.setImpuestos(extractDecimal(request.documento(), "impuestos"));
        documento.setTotal(extractDecimal(request.documento(), "total"));
        documento.setJsonOriginal(writeJson(request));
        documento.setRequiereIntervencion(false);
        documento.setIntentosProcesamiento(0);
        documento.setFechaRecepcion(LocalDateTime.now());

        DocumentoElectronico guardado = documentoElectronicoRepository.save(documento);
        registrarHistorialInicial(guardado);
        archivoDocumentoService.guardarJsonOriginal(guardado);

        return new DocumentoRecepcionResponse(
                guardado.getUuid().toString(),
                guardado.getTipoDocumento().name(),
                DocumentoEstado.RECIBIDO.name(),
                "Documento recibido para procesamiento",
                false
        );
    }

    private void validarContratoBase(DocumentoRecepcionRequest request) {
        if (blankToNull(request.externalId()) == null) {
            throw new DocumentoRecepcionException("El campo externalId es obligatorio");
        }
    }

    private void registrarHistorialInicial(DocumentoElectronico documento) {
        DocumentoEstadoHistorial historial = new DocumentoEstadoHistorial();
        historial.setDocumento(documento);
        historial.setEstadoAnterior(null);
        historial.setEstadoNuevo(DocumentoEstado.RECIBIDO);
        historial.setDescripcion("Documento recibido por API");
        historial.setOrigen("API");
        historial.setCreatedAt(LocalDateTime.now());
        historialRepository.save(historial);
    }

    private Optional<DocumentoElectronico> buscarDuplicado(Long empresaId, String externalId, String idempotencyKey) {
        String external = blankToNull(externalId);
        if (external != null) {
            Optional<DocumentoElectronico> byExternal = documentoElectronicoRepository.findByEmpresaIdAndExternalId(empresaId, external);
            if (byExternal.isPresent()) {
                return byExternal;
            }
        }

        String idem = blankToNull(idempotencyKey);
        if (idem != null) {
            return documentoElectronicoRepository.findByEmpresaIdAndIdempotencyKey(empresaId, idem);
        }
        return Optional.empty();
    }

    private Empresa resolverEmpresa(DocumentoRecepcionRequest request) {
        String ruc = extractText(request.emisor(), "ruc");
        if (ruc == null) {
            throw new DocumentoRecepcionException("El campo emisor.ruc es obligatorio para registrar el documento");
        }
        return empresaRepository.findByRuc(ruc)
                .orElseThrow(() -> new DocumentoRecepcionException("No existe una empresa configurada para el RUC " + ruc));
    }

    private TipoDocumento parseTipoDocumento(String tipoDocumento) {
        try {
            return TipoDocumento.valueOf(tipoDocumento.trim().toUpperCase());
        } catch (Exception ex) {
            throw new DocumentoRecepcionException("Tipo de documento no soportado: " + tipoDocumento);
        }
    }

    private short resolveAmbiente(DocumentoRecepcionRequest request, Empresa empresa) {
        String ambiente = extractText(request.emisor(), "ambiente");
        if (ambiente == null) {
            return empresa == null ? 1 : empresa.getSriAmbiente();
        }
        try {
            short value = Short.parseShort(ambiente);
            return (short) (value == 2 ? 2 : 1);
        } catch (NumberFormatException ex) {
            throw new DocumentoRecepcionException("El ambiente del emisor es invalido");
        }
    }

    private LocalDate resolveFechaEmision(DocumentoRecepcionRequest request) {
        String fecha = extractText(request.documento(), "fechaEmision");
        if (fecha == null) {
            throw new DocumentoRecepcionException("El campo documento.fechaEmision es obligatorio");
        }
        return LocalDate.parse(fecha);
    }

    private String resolveEmail(DocumentoRecepcionRequest request) {
        String emailReceptor = extractText(request.receptor(), "email");
        if (emailReceptor != null) {
            return emailReceptor;
        }
        if (request.correo() != null && request.correo().destinatarios() != null && !request.correo().destinatarios().isEmpty()) {
            return request.correo().destinatarios().get(0);
        }
        return null;
    }

    private String buildNumeroDocumento(String establecimiento, String puntoEmision, String secuencial) {
        if (establecimiento == null || puntoEmision == null || secuencial == null) {
            return null;
        }
        return establecimiento + "-" + puntoEmision + "-" + secuencial;
    }

    private String writeJson(DocumentoRecepcionRequest request) {
        try {
            return objectMapper.writeValueAsString(request);
        } catch (JsonProcessingException ex) {
            throw new DocumentoRecepcionException("No se pudo serializar el JSON original del documento");
        }
    }

    private String extractText(Map<String, Object> source, String key) {
        if (source == null) {
            return null;
        }
        Object value = source.get(key);
        if (value == null) {
            return null;
        }
        String text = String.valueOf(value).trim();
        return text.isEmpty() ? null : text;
    }

    private BigDecimal extractDecimal(Map<String, Object> source, String key) {
        String value = extractText(source, key);
        if (value == null) {
            return null;
        }
        try {
            return new BigDecimal(value);
        } catch (NumberFormatException ex) {
            throw new DocumentoRecepcionException("El valor numerico de " + key + " es invalido");
        }
    }

    private String blankToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }
}
