package com.erp.sri_files.service;

import com.erp.sri_files.domain.documento.DocumentoEstado;
import com.erp.sri_files.dto.response.CorreoPendienteItemResponse;
import com.erp.sri_files.dto.response.CorreoPendienteResponse;
import com.erp.sri_files.dto.response.MonitorComponenteEstadoResponse;
import com.erp.sri_files.dto.response.MonitorHealthResponse;
import com.erp.sri_files.dto.response.MonitorPendienteItemResponse;
import com.erp.sri_files.dto.response.MonitorPendientesResponse;
import com.erp.sri_files.dto.response.MonitorResumenResponse;
import com.erp.sri_files.repositories.documento.DocumentoElectronicoRepository;
import com.erp.sri_files.services.MailService;
import jakarta.persistence.EntityManager;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

@Service
public class MonitorService {

    private static final Set<DocumentoEstado> ESTADOS_ERROR = Set.of(
            DocumentoEstado.ERROR_VALIDACION,
            DocumentoEstado.ERROR_XML,
            DocumentoEstado.ERROR_FIRMA,
            DocumentoEstado.ERROR_ENVIO_SRI,
            DocumentoEstado.ERROR_AUTORIZACION,
            DocumentoEstado.ERROR_RIDE,
            DocumentoEstado.ERROR_CORREO,
            DocumentoEstado.DEVUELTO_SRI,
            DocumentoEstado.NO_AUTORIZADO,
            DocumentoEstado.REQUIERE_INTERVENCION
    );

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

    private static final Set<DocumentoEstado> ESTADOS_CORREO = Set.of(
            DocumentoEstado.CORREO_PENDIENTE,
            DocumentoEstado.ERROR_CORREO
    );

    private final DocumentoElectronicoRepository documentoElectronicoRepository;
    private final EntityManager entityManager;
    private final MailService mailService;
    private final String storageRoot;

    public MonitorService(
            DocumentoElectronicoRepository documentoElectronicoRepository,
            EntityManager entityManager,
            MailService mailService,
            @Value("${sri-files.storage.root:data/sri-files}") String storageRoot
    ) {
        this.documentoElectronicoRepository = documentoElectronicoRepository;
        this.entityManager = entityManager;
        this.mailService = mailService;
        this.storageRoot = storageRoot;
    }

    public MonitorHealthResponse obtenerHealth() {
        MonitorResumenResponse resumen = obtenerResumen();

        MonitorComponenteEstadoResponse database = validarBaseDatos();
        MonitorComponenteEstadoResponse storage = validarStorage();
        MonitorComponenteEstadoResponse email = validarCorreo();

        List<MonitorComponenteEstadoResponse> componentes = List.of(database, storage, email);
        boolean degradado = componentes.stream().anyMatch(item -> !"UP".equals(item.estado()));

        return new MonitorHealthResponse(
                degradado ? "DEGRADED" : "UP",
                LocalDateTime.now().toString(),
                resumen,
                componentes
        );
    }

    public MonitorResumenResponse obtenerResumen() {
        long total = documentoElectronicoRepository.count();
        long pendientesProcesamiento = contarPorEstados(Set.of(
                DocumentoEstado.RECIBIDO,
                DocumentoEstado.VALIDANDO,
                DocumentoEstado.VALIDADO,
                DocumentoEstado.XML_GENERADO,
                DocumentoEstado.FIRMADO,
                DocumentoEstado.ENVIANDO_SRI,
                DocumentoEstado.RECIBIDO_SRI
        ));
        long pendientesAutorizacion = contarPorEstados(Set.of(DocumentoEstado.PENDIENTE_AUTORIZACION));
        long pendientesCorreo = contarPorEstados(Set.of(DocumentoEstado.CORREO_PENDIENTE));
        long conError = contarPorEstados(ESTADOS_ERROR);
        long finalizados = contarPorEstados(Set.of(DocumentoEstado.FINALIZADO));

        return new MonitorResumenResponse(
                total,
                pendientesProcesamiento,
                pendientesAutorizacion,
                pendientesCorreo,
                conError,
                finalizados
        );
    }

    public MonitorPendientesResponse obtenerPendientes() {
        List<MonitorPendienteItemResponse> items = documentoElectronicoRepository.findAll().stream()
                .filter(documento -> documento.getEstadoActual() != null && ESTADOS_PENDIENTES.contains(documento.getEstadoActual()))
                .sorted((left, right) -> compararFechas(right.getFechaRecepcion(), left.getFechaRecepcion()))
                .map(documento -> new MonitorPendienteItemResponse(
                        documento.getUuid() != null ? documento.getUuid().toString() : null,
                        documento.getTipoDocumento() != null ? documento.getTipoDocumento().name() : null,
                        documento.getNumeroDocumento(),
                        documento.getRazonSocialReceptor(),
                        documento.getEstadoActual() != null ? documento.getEstadoActual().name() : null,
                        documento.getFechaRecepcion() != null ? documento.getFechaRecepcion().toString() : null,
                        documento.getIntentosProcesamiento(),
                        documento.isRequiereIntervencion()
                ))
                .toList();

        return new MonitorPendientesResponse(items.size(), items);
    }

    public CorreoPendienteResponse obtenerCorreosPendientes() {
        List<CorreoPendienteItemResponse> items = documentoElectronicoRepository.findAll().stream()
                .filter(documento -> documento.getEstadoActual() != null && ESTADOS_CORREO.contains(documento.getEstadoActual()))
                .sorted((left, right) -> compararFechas(right.getFechaAutorizacion(), left.getFechaAutorizacion()))
                .map(documento -> new CorreoPendienteItemResponse(
                        documento.getUuid() != null ? documento.getUuid().toString() : null,
                        documento.getTipoDocumento() != null ? documento.getTipoDocumento().name() : null,
                        documento.getNumeroDocumento(),
                        documento.getRazonSocialReceptor(),
                        documento.getEmailReceptor(),
                        documento.getEstadoActual() != null ? documento.getEstadoActual().name() : null,
                        documento.getFechaRecepcion() != null ? documento.getFechaRecepcion().toString() : null,
                        documento.getFechaAutorizacion() != null ? documento.getFechaAutorizacion().toString() : null,
                        documento.isRequiereIntervencion()
                ))
                .toList();

        return new CorreoPendienteResponse(items.size(), items);
    }

    private MonitorComponenteEstadoResponse validarBaseDatos() {
        try {
            entityManager.createNativeQuery("SELECT 1").getSingleResult();
            return new MonitorComponenteEstadoResponse("database", "UP", "Conexion operativa");
        } catch (Exception ex) {
            return new MonitorComponenteEstadoResponse("database", "DOWN", ex.getMessage());
        }
    }

    private MonitorComponenteEstadoResponse validarStorage() {
        try {
            Path root = Path.of(storageRoot);
            Files.createDirectories(root);
            boolean writable = Files.isWritable(root);
            return new MonitorComponenteEstadoResponse(
                    "storage",
                    writable ? "UP" : "DOWN",
                    writable ? root.toAbsolutePath().toString() : "La ruta no tiene permisos de escritura"
            );
        } catch (Exception ex) {
            return new MonitorComponenteEstadoResponse("storage", "DOWN", ex.getMessage());
        }
    }

    private MonitorComponenteEstadoResponse validarCorreo() {
        boolean healthy = mailService.smtpHealth();
        return new MonitorComponenteEstadoResponse(
                "smtp",
                healthy ? "UP" : "DOWN",
                healthy ? "SMTP operativo desde sri-files" : "No fue posible conectar con el SMTP configurado"
        );
    }

    private long contarPorEstados(Set<DocumentoEstado> estados) {
        return documentoElectronicoRepository.findAll().stream()
                .filter(documento -> documento.getEstadoActual() != null && estados.contains(documento.getEstadoActual()))
                .count();
    }

    private int compararFechas(LocalDateTime left, LocalDateTime right) {
        if (left == null && right == null) {
            return 0;
        }
        if (left == null) {
            return -1;
        }
        if (right == null) {
            return 1;
        }
        return left.compareTo(right);
    }
}
