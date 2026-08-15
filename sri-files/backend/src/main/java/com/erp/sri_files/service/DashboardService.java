package com.erp.sri_files.service;

import com.erp.sri_files.domain.documento.DocumentoElectronico;
import com.erp.sri_files.domain.documento.DocumentoEstado;
import com.erp.sri_files.dto.response.DashboardDocumentoDiaResponse;
import com.erp.sri_files.dto.response.DashboardDocumentoEstadoResponse;
import com.erp.sri_files.dto.response.DashboardDocumentoTipoResponse;
import com.erp.sri_files.dto.response.DashboardErrorEtapaResponse;
import com.erp.sri_files.dto.response.DashboardResumenResponse;
import com.erp.sri_files.dto.response.DashboardTiemposResponse;
import com.erp.sri_files.repositories.documento.DocumentoElectronicoRepository;
import com.erp.sri_files.repositories.documento.DocumentoErrorRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class DashboardService {
    private static final Set<DocumentoEstado> ESTADOS_PROCESANDO = Set.of(
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
            DocumentoEstado.CORREO_ENVIADO,
            DocumentoEstado.FINALIZADO
    );

    private static final Set<DocumentoEstado> ESTADOS_NO_AUTORIZADOS = Set.of(
            DocumentoEstado.NO_AUTORIZADO,
            DocumentoEstado.DEVUELTO_SRI,
            DocumentoEstado.CANCELADO
    );

    private static final Set<DocumentoEstado> ESTADOS_ERROR = Set.of(
            DocumentoEstado.ERROR_VALIDACION,
            DocumentoEstado.ERROR_XML,
            DocumentoEstado.ERROR_FIRMA,
            DocumentoEstado.ERROR_ENVIO_SRI,
            DocumentoEstado.ERROR_AUTORIZACION,
            DocumentoEstado.ERROR_RIDE,
            DocumentoEstado.ERROR_CORREO
    );

    private final DocumentoElectronicoRepository documentoRepository;
    private final DocumentoErrorRepository documentoErrorRepository;

    public DashboardService(
            DocumentoElectronicoRepository documentoRepository,
            DocumentoErrorRepository documentoErrorRepository
    ) {
        this.documentoRepository = documentoRepository;
        this.documentoErrorRepository = documentoErrorRepository;
    }

    @Transactional(readOnly = true)
    public DashboardResumenResponse obtenerResumen(String empresaUuid) {
        List<DocumentoElectronico> documentos = filtrarPorEmpresa(empresaUuid);
        LocalDate hoy = LocalDate.now();

        long recibidos = documentos.stream()
                .filter(documento -> documento.getFechaRecepcion() != null)
                .filter(documento -> hoy.equals(documento.getFechaRecepcion().toLocalDate()))
                .count();

        return new DashboardResumenResponse(
                documentos.size(),
                recibidos,
                contar(documentos, ESTADOS_PROCESANDO),
                contar(documentos, ESTADOS_AUTORIZADOS),
                contar(documentos, ESTADOS_NO_AUTORIZADOS),
                contar(documentos, ESTADOS_ERROR),
                contar(documentos, Set.of(DocumentoEstado.CORREO_PENDIENTE))
        );
    }

    @Transactional(readOnly = true)
    public List<DashboardDocumentoTipoResponse> obtenerDocumentosPorTipo(String empresaUuid) {
        return agrupar(filtrarPorEmpresa(empresaUuid), documento -> documento.getTipoDocumento().name())
                .stream()
                .map(entry -> new DashboardDocumentoTipoResponse(entry.getKey(), entry.getValue()))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<DashboardDocumentoEstadoResponse> obtenerDocumentosPorEstado(String empresaUuid) {
        return agrupar(filtrarPorEmpresa(empresaUuid), documento -> documento.getEstadoActual().name())
                .stream()
                .map(entry -> new DashboardDocumentoEstadoResponse(entry.getKey(), entry.getValue()))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<DashboardDocumentoDiaResponse> obtenerDocumentosPorDia(String empresaUuid) {
        return agrupar(
                filtrarPorEmpresa(empresaUuid).stream()
                        .filter(documento -> documento.getFechaRecepcion() != null)
                        .toList(),
                documento -> documento.getFechaRecepcion().toLocalDate().toString()
        ).stream()
                .map(entry -> new DashboardDocumentoDiaResponse(entry.getKey(), entry.getValue()))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<DashboardErrorEtapaResponse> obtenerErroresPorEtapa(String empresaUuid) {
        Map<String, Long> conteos = documentoErrorRepository.findAll().stream()
                .filter(error -> empresaUuid == null || empresaUuid.isBlank()
                        || (error.getDocumento() != null
                        && error.getDocumento().getEmpresa() != null
                        && error.getDocumento().getEmpresa().getUuid().equals(UUID.fromString(empresaUuid.trim()))))
                .collect(Collectors.groupingBy(
                        error -> error.getEtapa().name(),
                        Collectors.counting()
                ));

        return conteos.entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed()
                        .thenComparing(Map.Entry.comparingByKey()))
                .map(entry -> new DashboardErrorEtapaResponse(entry.getKey(), entry.getValue()))
                .toList();
    }

    @Transactional(readOnly = true)
    public DashboardTiemposResponse obtenerTiempos(String empresaUuid) {
        List<DocumentoElectronico> documentos = filtrarPorEmpresa(empresaUuid);

        long promedioProcesamientoMs = calcularPromedio(
                documentos,
                DocumentoElectronico::getFechaInicioProcesamiento,
                DocumentoElectronico::getFechaFinalizacion
        );

        long promedioAutorizacionMs = calcularPromedio(
                documentos,
                DocumentoElectronico::getFechaRecepcion,
                DocumentoElectronico::getFechaAutorizacion
        );

        return new DashboardTiemposResponse(promedioProcesamientoMs, promedioAutorizacionMs);
    }

    private long contar(List<DocumentoElectronico> documentos, Set<DocumentoEstado> estados) {
        return documentos.stream()
                .filter(documento -> estados.contains(documento.getEstadoActual()))
                .count();
    }

    private List<DocumentoElectronico> filtrarPorEmpresa(String empresaUuid) {
        if (empresaUuid == null || empresaUuid.isBlank()) {
            return documentoRepository.findAll();
        }

        UUID empresaId = UUID.fromString(empresaUuid.trim());
        return documentoRepository.findAll().stream()
                .filter(documento -> documento.getEmpresa() != null && empresaId.equals(documento.getEmpresa().getUuid()))
                .toList();
    }

    private List<Map.Entry<String, Long>> agrupar(
            List<DocumentoElectronico> documentos,
            Function<DocumentoElectronico, String> classifier
    ) {
        return documentos.stream()
                .map(documento -> Map.entry(classifier.apply(documento), documento))
                .filter(entry -> entry.getKey() != null && !entry.getKey().isBlank())
                .collect(Collectors.groupingBy(Map.Entry::getKey, Collectors.counting()))
                .entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed()
                        .thenComparing(Map.Entry.comparingByKey()))
                .toList();
    }

    private long calcularPromedio(
            List<DocumentoElectronico> documentos,
            Function<DocumentoElectronico, LocalDateTime> inicio,
            Function<DocumentoElectronico, LocalDateTime> fin
    ) {
        return Math.round(documentos.stream()
                .map(documento -> calcularDuracion(documento, inicio, fin))
                .filter(valor -> valor >= 0)
                .mapToLong(Long::longValue)
                .average()
                .orElse(0));
    }

    private long calcularDuracion(
            DocumentoElectronico documento,
            Function<DocumentoElectronico, LocalDateTime> inicio,
            Function<DocumentoElectronico, LocalDateTime> fin
    ) {
        LocalDateTime fechaInicio = inicio.apply(documento);
        LocalDateTime fechaFin = fin.apply(documento);
        if (fechaInicio == null || fechaFin == null || fechaFin.isBefore(fechaInicio)) {
            return -1;
        }

        return Duration.between(fechaInicio, fechaFin).toMillis();
    }
}
