package com.erp.sri_files.service;

import com.erp.sri_files.domain.documento.DocumentoElectronico;
import com.erp.sri_files.domain.documento.DocumentoEstado;
import com.erp.sri_files.domain.documento.DocumentoEstadoHistorial;
import com.erp.sri_files.exceptions.DocumentoRecepcionException;
import com.erp.sri_files.repositories.documento.DocumentoElectronicoRepository;
import com.erp.sri_files.repositories.documento.DocumentoEstadoHistorialRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

@Service
public class EstadoDocumentoService {

    private static final Map<DocumentoEstado, Set<DocumentoEstado>> TRANSICIONES = createTransitions();

    private final DocumentoElectronicoRepository documentoRepository;
    private final DocumentoEstadoHistorialRepository historialRepository;

    public EstadoDocumentoService(
            DocumentoElectronicoRepository documentoRepository,
            DocumentoEstadoHistorialRepository historialRepository
    ) {
        this.documentoRepository = documentoRepository;
        this.historialRepository = historialRepository;
    }

    @Transactional
    public DocumentoElectronico cambiar(DocumentoElectronico documento, DocumentoEstado nuevoEstado, String descripcion) {
        DocumentoEstado actual = documento.getEstadoActual();
        if (actual != null && !transicionPermitida(actual, nuevoEstado)) {
            throw new DocumentoRecepcionException("Transicion de estado no permitida: " + actual + " -> " + nuevoEstado);
        }

        documento.setEstadoActual(nuevoEstado);
        if (nuevoEstado == DocumentoEstado.REQUIERE_INTERVENCION) {
            documento.setRequiereIntervencion(true);
        }
        DocumentoElectronico guardado = documentoRepository.save(documento);

        DocumentoEstadoHistorial historial = new DocumentoEstadoHistorial();
        historial.setDocumento(guardado);
        historial.setEstadoAnterior(actual);
        historial.setEstadoNuevo(nuevoEstado);
        historial.setDescripcion(descripcion);
        historial.setOrigen("SYSTEM");
        historial.setCreatedAt(LocalDateTime.now());
        historialRepository.save(historial);

        return guardado;
    }

    @Transactional
    public DocumentoElectronico forzarCambio(DocumentoElectronico documento, DocumentoEstado nuevoEstado, String descripcion, String origen) {
        DocumentoEstado actual = documento.getEstadoActual();
        documento.setEstadoActual(nuevoEstado);
        documento.setRequiereIntervencion(false);
        DocumentoElectronico guardado = documentoRepository.save(documento);

        DocumentoEstadoHistorial historial = new DocumentoEstadoHistorial();
        historial.setDocumento(guardado);
        historial.setEstadoAnterior(actual);
        historial.setEstadoNuevo(nuevoEstado);
        historial.setDescripcion(descripcion);
        historial.setOrigen(origen);
        historial.setCreatedAt(LocalDateTime.now());
        historialRepository.save(historial);

        return guardado;
    }

    private boolean transicionPermitida(DocumentoEstado actual, DocumentoEstado nuevoEstado) {
        Set<DocumentoEstado> permitidos = TRANSICIONES.get(actual);
        return permitidos != null && permitidos.contains(nuevoEstado);
    }

    private static Map<DocumentoEstado, Set<DocumentoEstado>> createTransitions() {
        Map<DocumentoEstado, Set<DocumentoEstado>> transitions = new HashMap<>();
        transitions.put(DocumentoEstado.RECIBIDO, EnumSet.of(DocumentoEstado.VALIDANDO, DocumentoEstado.CANCELADO));
        transitions.put(DocumentoEstado.VALIDANDO, EnumSet.of(DocumentoEstado.VALIDADO, DocumentoEstado.ERROR_VALIDACION));
        transitions.put(DocumentoEstado.VALIDADO, EnumSet.of(DocumentoEstado.XML_GENERADO, DocumentoEstado.ERROR_XML));
        transitions.put(DocumentoEstado.XML_GENERADO, EnumSet.of(DocumentoEstado.FIRMADO, DocumentoEstado.ERROR_FIRMA));
        transitions.put(DocumentoEstado.FIRMADO, EnumSet.of(DocumentoEstado.ENVIANDO_SRI, DocumentoEstado.ERROR_ENVIO_SRI));
        transitions.put(DocumentoEstado.ENVIANDO_SRI, EnumSet.of(DocumentoEstado.RECIBIDO_SRI, DocumentoEstado.DEVUELTO_SRI, DocumentoEstado.ERROR_ENVIO_SRI));
        transitions.put(DocumentoEstado.RECIBIDO_SRI, EnumSet.of(DocumentoEstado.PENDIENTE_AUTORIZACION));
        transitions.put(DocumentoEstado.PENDIENTE_AUTORIZACION, EnumSet.of(DocumentoEstado.AUTORIZADO, DocumentoEstado.NO_AUTORIZADO, DocumentoEstado.ERROR_AUTORIZACION));
        transitions.put(DocumentoEstado.AUTORIZADO, EnumSet.of(DocumentoEstado.RIDE_GENERADO, DocumentoEstado.ERROR_RIDE));
        transitions.put(DocumentoEstado.RIDE_GENERADO, EnumSet.of(DocumentoEstado.CORREO_PENDIENTE, DocumentoEstado.FINALIZADO));
        transitions.put(DocumentoEstado.CORREO_PENDIENTE, EnumSet.of(DocumentoEstado.CORREO_ENVIADO, DocumentoEstado.ERROR_CORREO));
        transitions.put(DocumentoEstado.CORREO_ENVIADO, EnumSet.of(DocumentoEstado.FINALIZADO));
        transitions.put(DocumentoEstado.ERROR_XML, EnumSet.of(DocumentoEstado.REQUIERE_INTERVENCION));
        transitions.put(DocumentoEstado.ERROR_VALIDACION, EnumSet.of(DocumentoEstado.REQUIERE_INTERVENCION));
        transitions.put(DocumentoEstado.ERROR_FIRMA, EnumSet.of(DocumentoEstado.REQUIERE_INTERVENCION));
        transitions.put(DocumentoEstado.ERROR_ENVIO_SRI, EnumSet.of(DocumentoEstado.REQUIERE_INTERVENCION));
        transitions.put(DocumentoEstado.ERROR_AUTORIZACION, EnumSet.of(DocumentoEstado.REQUIERE_INTERVENCION));
        transitions.put(DocumentoEstado.ERROR_RIDE, EnumSet.of(DocumentoEstado.REQUIERE_INTERVENCION));
        transitions.put(DocumentoEstado.ERROR_CORREO, EnumSet.of(DocumentoEstado.REQUIERE_INTERVENCION));
        return Collections.unmodifiableMap(transitions);
    }
}
