package com.erp.sri_files.service;

import com.erp.sri_files.domain.documento.DocumentoElectronico;
import com.erp.sri_files.domain.documento.DocumentoEtapa;
import com.erp.sri_files.domain.documento.DocumentoError;
import com.erp.sri_files.domain.documento.DocumentoEstado;
import com.erp.sri_files.domain.documento.DocumentoEstadoHistorial;
import com.erp.sri_files.domain.documento.TipoDocumento;
import com.erp.sri_files.dto.response.DocumentoCorreoEventoResponse;
import com.erp.sri_files.dto.response.DocumentoCorreoSeguimientoResponse;
import com.erp.sri_files.dto.response.DocumentoAuditoriaEventoResponse;
import com.erp.sri_files.dto.response.DocumentoAuditoriaResumenResponse;
import com.erp.sri_files.dto.response.DocumentoDetalleResponse;
import com.erp.sri_files.dto.response.DocumentoErrorItemResponse;
import com.erp.sri_files.dto.response.DocumentoEstadoResponse;
import com.erp.sri_files.dto.response.DocumentoHistorialItemResponse;
import com.erp.sri_files.dto.response.DocumentoIntentoSriItemResponse;
import com.erp.sri_files.dto.response.DocumentoIntentoSriResponse;
import com.erp.sri_files.dto.response.DocumentoConteoResponse;
import com.erp.sri_files.dto.response.DocumentoListadoItemResponse;
import com.erp.sri_files.dto.response.DocumentoListadoResponse;
import com.erp.sri_files.dto.response.DocumentoResumenOperativoResponse;
import com.erp.sri_files.exceptions.DocumentoRecepcionException;
import com.erp.sri_files.repositories.documento.DocumentoElectronicoRepository;
import com.erp.sri_files.repositories.documento.DocumentoErrorRepository;
import com.erp.sri_files.repositories.documento.DocumentoEstadoHistorialRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class DocumentoConsultaService {
    private static final Set<DocumentoEstado> ESTADOS_PENDIENTES = Set.of(
            DocumentoEstado.RECIBIDO,
            DocumentoEstado.VALIDANDO,
            DocumentoEstado.VALIDADO,
            DocumentoEstado.XML_GENERADO,
            DocumentoEstado.FIRMADO,
            DocumentoEstado.ENVIANDO_SRI,
            DocumentoEstado.RECIBIDO_SRI,
            DocumentoEstado.PENDIENTE_AUTORIZACION
    );

    private static final Set<DocumentoEstado> ESTADOS_AUTORIZADOS = Set.of(
            DocumentoEstado.AUTORIZADO,
            DocumentoEstado.RIDE_GENERADO,
            DocumentoEstado.CORREO_PENDIENTE,
            DocumentoEstado.CORREO_ENVIADO,
            DocumentoEstado.FINALIZADO
    );

    private static final Set<DocumentoEstado> ESTADOS_ERROR = Set.of(
            DocumentoEstado.ERROR_VALIDACION,
            DocumentoEstado.ERROR_XML,
            DocumentoEstado.ERROR_FIRMA,
            DocumentoEstado.DEVUELTO_SRI,
            DocumentoEstado.NO_AUTORIZADO,
            DocumentoEstado.ERROR_ENVIO_SRI,
            DocumentoEstado.ERROR_AUTORIZACION,
            DocumentoEstado.ERROR_RIDE,
            DocumentoEstado.ERROR_CORREO,
            DocumentoEstado.CANCELADO
    );

    private static final Set<DocumentoEstado> ESTADOS_SRI_HISTORIAL = Set.of(
            DocumentoEstado.ENVIANDO_SRI,
            DocumentoEstado.RECIBIDO_SRI,
            DocumentoEstado.DEVUELTO_SRI,
            DocumentoEstado.PENDIENTE_AUTORIZACION,
            DocumentoEstado.AUTORIZADO,
            DocumentoEstado.NO_AUTORIZADO,
            DocumentoEstado.ERROR_ENVIO_SRI,
            DocumentoEstado.ERROR_AUTORIZACION
    );

    private static final Set<DocumentoEstado> ESTADOS_CORREO_HISTORIAL = Set.of(
            DocumentoEstado.CORREO_PENDIENTE,
            DocumentoEstado.CORREO_ENVIADO,
            DocumentoEstado.ERROR_CORREO,
            DocumentoEstado.FINALIZADO
    );

    private final DocumentoElectronicoRepository documentoRepository;
    private final DocumentoEstadoHistorialRepository historialRepository;
    private final DocumentoErrorRepository documentoErrorRepository;

    public DocumentoConsultaService(
            DocumentoElectronicoRepository documentoRepository,
            DocumentoEstadoHistorialRepository historialRepository,
            DocumentoErrorRepository documentoErrorRepository
    ) {
        this.documentoRepository = documentoRepository;
        this.historialRepository = historialRepository;
        this.documentoErrorRepository = documentoErrorRepository;
    }

    @Transactional(readOnly = true)
    public DocumentoDetalleResponse obtener(UUID uuid) {
        DocumentoElectronico documento = buscar(uuid);
        return new DocumentoDetalleResponse(
                documento.getUuid().toString(),
                documento.getTipoDocumento().name(),
                documento.getEstadoActual().name(),
                documento.getExternalId(),
                documento.getNumeroDocumento(),
                documento.getClaveAcceso(),
                documento.getIdentificacionReceptor(),
                documento.getRazonSocialReceptor(),
                documento.getEmailReceptor(),
                documento.getFechaEmision(),
                documento.getFechaRecepcion()
        );
    }

    @Transactional(readOnly = true)
    public DocumentoEstadoResponse obtenerEstado(UUID uuid) {
        DocumentoElectronico documento = buscar(uuid);
        return new DocumentoEstadoResponse(
                documento.getUuid().toString(),
                documento.getEstadoActual().name(),
                documento.isRequiereIntervencion()
        );
    }

    @Transactional(readOnly = true)
    public DocumentoListadoResponse listar(
            String empresaUuid,
            String tipoDocumento,
            String estado,
            String busqueda,
            int page,
            int size
    ) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "fechaRecepcion"));
        Page<DocumentoElectronico> documentos = documentoRepository.findAll(
                aplicarFiltros(empresaUuid, tipoDocumento, estado, busqueda),
                pageable
        );

        return new DocumentoListadoResponse(
                documentos.getContent().stream().map(this::mapearListadoItem).toList(),
                documentos.getNumber(),
                documentos.getSize(),
                documentos.getTotalElements(),
                documentos.getTotalPages()
        );
    }

    @Transactional(readOnly = true)
    public DocumentoResumenOperativoResponse obtenerResumenOperativo() {
        List<DocumentoElectronico> documentos = documentoRepository.findAll();
        LocalDate hoy = LocalDate.now();

        long totalDocumentos = documentos.size();
        long recibidosHoy = documentos.stream()
                .filter(documento -> documento.getFechaRecepcion() != null)
                .filter(documento -> hoy.equals(documento.getFechaRecepcion().toLocalDate()))
                .count();
        long autorizados = contarPorEstado(documentos, ESTADOS_AUTORIZADOS);
        long pendientes = contarPorEstado(documentos, ESTADOS_PENDIENTES);
        long conErrores = contarPorEstado(documentos, ESTADOS_ERROR);
        long requiereIntervencion = documentos.stream()
                .filter(DocumentoElectronico::isRequiereIntervencion)
                .count();

        return new DocumentoResumenOperativoResponse(
                totalDocumentos,
                recibidosHoy,
                autorizados,
                pendientes,
                conErrores,
                requiereIntervencion,
                agrupar(documentos, documento -> documento.getTipoDocumento().name()),
                agrupar(documentos, documento -> documento.getEstadoActual().name())
        );
    }

    @Transactional(readOnly = true)
    public List<DocumentoHistorialItemResponse> obtenerHistorial(UUID uuid) {
        buscar(uuid);
        return historialRepository.findByDocumento_UuidOrderByCreatedAtDesc(uuid).stream()
                .map(this::mapearHistorialItem)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<DocumentoErrorItemResponse> obtenerErrores(UUID uuid) {
        buscar(uuid);
        return documentoErrorRepository.findByDocumento_UuidOrderByCreatedAtDesc(uuid).stream()
                .map(this::mapearErrorItem)
                .toList();
    }

    @Transactional(readOnly = true)
    public DocumentoIntentoSriResponse obtenerIntentosSri(UUID uuid) {
        DocumentoElectronico documento = buscar(uuid);

        List<DocumentoIntentoSriItemResponse> eventos = new ArrayList<>();

        historialRepository.findByDocumento_UuidOrderByCreatedAtDesc(uuid).stream()
                .filter(item -> item.getEstadoNuevo() != null && ESTADOS_SRI_HISTORIAL.contains(item.getEstadoNuevo()))
                .map(this::mapearIntentoDesdeHistorial)
                .forEach(eventos::add);

        documentoErrorRepository.findByDocumento_UuidOrderByCreatedAtDesc(uuid).stream()
                .filter(item -> item.getEtapa() == DocumentoEtapa.ENVIO_SRI || item.getEtapa() == DocumentoEtapa.AUTORIZACION)
                .map(this::mapearIntentoDesdeError)
                .forEach(eventos::add);

        eventos.sort(Comparator.comparing(
                DocumentoIntentoSriItemResponse::createdAt,
                Comparator.nullsLast(Comparator.reverseOrder())
        ));

        return new DocumentoIntentoSriResponse(
                documento.getUuid().toString(),
                documento.getIntentosProcesamiento(),
                documento.getEstadoActual() != null ? documento.getEstadoActual().name() : null,
                documento.isRequiereIntervencion(),
                documento.getFechaInicioProcesamiento() != null ? documento.getFechaInicioProcesamiento().toString() : null,
                documento.getFechaFinalizacion() != null ? documento.getFechaFinalizacion().toString() : null,
                eventos
        );
    }

    @Transactional(readOnly = true)
    public DocumentoCorreoSeguimientoResponse obtenerSeguimientoCorreo(UUID uuid) {
        DocumentoElectronico documento = buscar(uuid);

        List<DocumentoCorreoEventoResponse> eventos = new ArrayList<>();

        historialRepository.findByDocumento_UuidOrderByCreatedAtDesc(uuid).stream()
                .filter(item -> item.getEstadoNuevo() != null && ESTADOS_CORREO_HISTORIAL.contains(item.getEstadoNuevo()))
                .map(this::mapearCorreoDesdeHistorial)
                .forEach(eventos::add);

        documentoErrorRepository.findByDocumento_UuidOrderByCreatedAtDesc(uuid).stream()
                .filter(item -> item.getEtapa() == DocumentoEtapa.CORREO)
                .map(this::mapearCorreoDesdeError)
                .forEach(eventos::add);

        eventos.sort(Comparator.comparing(
                DocumentoCorreoEventoResponse::createdAt,
                Comparator.nullsLast(Comparator.reverseOrder())
        ));

        String remitente = documento.getEmpresa() != null ? documento.getEmpresa().getCorreoNotificaciones() : null;
        String destinatario = documento.getEmailReceptor();

        return new DocumentoCorreoSeguimientoResponse(
                documento.getUuid().toString(),
                destinatario,
                remitente,
                documento.getEstadoActual() != null ? documento.getEstadoActual().name() : null,
                documento.isRequiereIntervencion(),
                destinatario != null && !destinatario.isBlank(),
                eventos
        );
    }

    @Transactional(readOnly = true)
    public DocumentoAuditoriaResumenResponse obtenerAuditoriaReciente() {
        List<DocumentoAuditoriaEventoResponse> eventos = historialRepository.findTop100ByOrderByCreatedAtDesc().stream()
                .map(this::mapearAuditoriaEvento)
                .toList();

        return new DocumentoAuditoriaResumenResponse(eventos.size(), eventos);
    }

    private Specification<DocumentoElectronico> aplicarFiltros(String empresaUuid, String tipoDocumento, String estado, String busqueda) {
        Specification<DocumentoElectronico> spec = Specification.where(null);

        if (empresaUuid != null && !empresaUuid.isBlank()) {
            UUID empresaId = UUID.fromString(empresaUuid.trim());
            spec = spec.and((root, query, cb) -> cb.equal(root.get("empresa").get("uuid"), empresaId));
        }

        if (tipoDocumento != null && !tipoDocumento.isBlank()) {
            TipoDocumento tipo = TipoDocumento.valueOf(tipoDocumento.trim().toUpperCase());
            spec = spec.and((root, query, cb) -> cb.equal(root.get("tipoDocumento"), tipo));
        }

        if (estado != null && !estado.isBlank()) {
            DocumentoEstado estadoDocumento = DocumentoEstado.valueOf(estado.trim().toUpperCase());
            spec = spec.and((root, query, cb) -> cb.equal(root.get("estadoActual"), estadoDocumento));
        }

        if (busqueda != null && !busqueda.isBlank()) {
            String criterio = "%" + busqueda.trim().toLowerCase() + "%";
            spec = spec.and((root, query, cb) -> cb.or(
                    cb.like(cb.lower(root.get("numeroDocumento")), criterio),
                    cb.like(cb.lower(root.get("razonSocialReceptor")), criterio),
                    cb.like(cb.lower(root.get("identificacionReceptor")), criterio),
                    cb.like(cb.lower(root.get("externalId")), criterio)
            ));
        }

        return spec;
    }

    private DocumentoListadoItemResponse mapearListadoItem(DocumentoElectronico documento) {
        return new DocumentoListadoItemResponse(
                documento.getUuid().toString(),
                documento.getTipoDocumento().name(),
                documento.getNumeroDocumento(),
                documento.getRazonSocialReceptor(),
                documento.getFechaEmision() != null ? documento.getFechaEmision().toString() : null,
                documento.getEstadoActual().name()
        );
    }

    private DocumentoHistorialItemResponse mapearHistorialItem(DocumentoEstadoHistorial historial) {
        return new DocumentoHistorialItemResponse(
                historial.getId(),
                historial.getEstadoAnterior() != null ? historial.getEstadoAnterior().name() : null,
                historial.getEstadoNuevo() != null ? historial.getEstadoNuevo().name() : null,
                historial.getDescripcion(),
                historial.getOrigen(),
                historial.getUsuarioId(),
                historial.getMetadata(),
                historial.getCreatedAt() != null ? historial.getCreatedAt().toString() : null
        );
    }

    private DocumentoAuditoriaEventoResponse mapearAuditoriaEvento(DocumentoEstadoHistorial historial) {
        DocumentoElectronico documento = historial.getDocumento();
        return new DocumentoAuditoriaEventoResponse(
                historial.getId(),
                documento != null && documento.getUuid() != null ? documento.getUuid().toString() : null,
                documento != null && documento.getTipoDocumento() != null ? documento.getTipoDocumento().name() : null,
                documento != null ? documento.getNumeroDocumento() : null,
                documento != null ? documento.getExternalId() : null,
                historial.getEstadoAnterior() != null ? historial.getEstadoAnterior().name() : null,
                historial.getEstadoNuevo() != null ? historial.getEstadoNuevo().name() : null,
                historial.getDescripcion(),
                historial.getOrigen(),
                historial.getCreatedAt() != null ? historial.getCreatedAt().toString() : null
        );
    }

    private DocumentoErrorItemResponse mapearErrorItem(DocumentoError error) {
        return new DocumentoErrorItemResponse(
                error.getId(),
                error.getEtapa() != null ? error.getEtapa().name() : null,
                error.getCodigo(),
                error.getMensaje(),
                error.getDetalle(),
                error.isRecuperable(),
                error.isResuelto(),
                error.getFechaResolucion() != null ? error.getFechaResolucion().toString() : null,
                error.getCreatedAt() != null ? error.getCreatedAt().toString() : null
        );
    }

    private DocumentoIntentoSriItemResponse mapearIntentoDesdeHistorial(DocumentoEstadoHistorial historial) {
        return new DocumentoIntentoSriItemResponse(
                "HISTORIAL",
                inferirEtapaSri(historial.getEstadoNuevo()),
                historial.getEstadoNuevo() != null ? historial.getEstadoNuevo().name() : null,
                resolverResultado(historial.getEstadoNuevo()),
                historial.getDescripcion(),
                null,
                historial.getMetadata(),
                false,
                historial.getCreatedAt() != null ? historial.getCreatedAt().toString() : null
        );
    }

    private DocumentoIntentoSriItemResponse mapearIntentoDesdeError(DocumentoError error) {
        return new DocumentoIntentoSriItemResponse(
                "ERROR",
                error.getEtapa() != null ? error.getEtapa().name() : null,
                null,
                error.isResuelto() ? "RESUELTO" : "ERROR",
                error.getDetalle(),
                error.getCodigo(),
                error.getMensaje(),
                error.isRecuperable(),
                error.getCreatedAt() != null ? error.getCreatedAt().toString() : null
        );
    }

    private DocumentoCorreoEventoResponse mapearCorreoDesdeHistorial(DocumentoEstadoHistorial historial) {
        DocumentoEstado estadoNuevo = historial.getEstadoNuevo();
        return new DocumentoCorreoEventoResponse(
                "HISTORIAL",
                estadoNuevo != null ? estadoNuevo.name() : null,
                resolverResultadoCorreo(estadoNuevo),
                historial.getDescripcion(),
                null,
                historial.getMetadata(),
                false,
                historial.getCreatedAt() != null ? historial.getCreatedAt().toString() : null
        );
    }

    private DocumentoCorreoEventoResponse mapearCorreoDesdeError(DocumentoError error) {
        return new DocumentoCorreoEventoResponse(
                "ERROR",
                DocumentoEstado.ERROR_CORREO.name(),
                error.isResuelto() ? "RESUELTO" : "ERROR",
                error.getDetalle(),
                error.getCodigo(),
                error.getMensaje(),
                error.isRecuperable(),
                error.getCreatedAt() != null ? error.getCreatedAt().toString() : null
        );
    }

    private String inferirEtapaSri(DocumentoEstado estado) {
        if (estado == null) {
            return null;
        }

        return switch (estado) {
            case ENVIANDO_SRI, RECIBIDO_SRI, DEVUELTO_SRI, ERROR_ENVIO_SRI -> DocumentoEtapa.ENVIO_SRI.name();
            case PENDIENTE_AUTORIZACION, AUTORIZADO, NO_AUTORIZADO, ERROR_AUTORIZACION -> DocumentoEtapa.AUTORIZACION.name();
            default -> null;
        };
    }

    private String resolverResultado(DocumentoEstado estado) {
        if (estado == null) {
            return null;
        }

        return switch (estado) {
            case AUTORIZADO, RECIBIDO_SRI -> "OK";
            case DEVUELTO_SRI, NO_AUTORIZADO, ERROR_ENVIO_SRI, ERROR_AUTORIZACION -> "ERROR";
            case ENVIANDO_SRI, PENDIENTE_AUTORIZACION -> "PENDIENTE";
            default -> estado.name();
        };
    }

    private String resolverResultadoCorreo(DocumentoEstado estado) {
        if (estado == null) {
            return null;
        }

        return switch (estado) {
            case CORREO_ENVIADO, FINALIZADO -> "OK";
            case CORREO_PENDIENTE -> "PENDIENTE";
            case ERROR_CORREO -> "ERROR";
            default -> estado.name();
        };
    }

    private long contarPorEstado(List<DocumentoElectronico> documentos, Set<DocumentoEstado> estados) {
        return documentos.stream()
                .filter(documento -> estados.contains(documento.getEstadoActual()))
                .count();
    }

    private List<DocumentoConteoResponse> agrupar(
            List<DocumentoElectronico> documentos,
            Function<DocumentoElectronico, String> classifier
    ) {
        Map<String, Long> conteos = documentos.stream()
                .collect(Collectors.groupingBy(classifier, Collectors.counting()));

        return conteos.entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed()
                        .thenComparing(Map.Entry.comparingByKey()))
                .map(entry -> new DocumentoConteoResponse(entry.getKey(), entry.getValue()))
                .toList();
    }

    private DocumentoElectronico buscar(UUID uuid) {
        return documentoRepository.findByUuid(uuid)
                .orElseThrow(() -> new DocumentoRecepcionException("No existe documento con uuid " + uuid));
    }
}
