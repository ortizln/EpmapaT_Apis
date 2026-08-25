package com.erp.sri_files.service;

import com.erp.sri_files.domain.documento.DocumentoElectronico;
import com.erp.sri_files.domain.documento.DocumentoEstado;
import com.erp.sri_files.dto.response.MonitorHealthResponse;
import com.erp.sri_files.dto.response.MonitorResumenResponse;
import com.erp.sri_files.repositories.documento.DocumentoElectronicoRepository;
import com.erp.sri_files.services.MailService;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class MonitorServiceTest {

    @Test
    void generaResumenConConteosOperativos() throws Exception {
        DocumentoElectronico recibido = documentoConEstado(DocumentoEstado.RECIBIDO);
        DocumentoElectronico pendienteAutorizacion = documentoConEstado(DocumentoEstado.PENDIENTE_AUTORIZACION);
        DocumentoElectronico correoPendiente = documentoConEstado(DocumentoEstado.CORREO_PENDIENTE);
        DocumentoElectronico conError = documentoConEstado(DocumentoEstado.ERROR_XML);
        DocumentoElectronico finalizado = documentoConEstado(DocumentoEstado.FINALIZADO);

        DocumentoElectronicoRepository repository = mock(DocumentoElectronicoRepository.class);
        EntityManager entityManager = mock(EntityManager.class);
        Query query = mock(Query.class);
        MailService mailService = mock(MailService.class);
        Path tempDir = Files.createTempDirectory("sri-monitor-test");

        when(repository.count()).thenReturn(5L);
        when(repository.findAll()).thenReturn(List.of(recibido, pendienteAutorizacion, correoPendiente, conError, finalizado));
        when(entityManager.createNativeQuery("SELECT 1")).thenReturn(query);
        when(query.getSingleResult()).thenReturn(1);
        when(mailService.smtpHealth()).thenReturn(true);

        MonitorService service = new MonitorService(repository, entityManager, mailService, tempDir.toString());

        MonitorResumenResponse resumen = service.obtenerResumen();
        MonitorHealthResponse health = service.obtenerHealth();

        assertEquals(5, resumen.totalDocumentos());
        assertEquals(1, resumen.pendientesProcesamiento());
        assertEquals(1, resumen.pendientesAutorizacion());
        assertEquals(1, resumen.pendientesCorreo());
        assertEquals(1, resumen.conError());
        assertEquals(1, resumen.finalizados());
        assertEquals("UP", health.estado());
        assertNotNull(health.timestamp());
        assertEquals(3, health.componentes().size());
        assertTrue(health.componentes().stream().allMatch(item -> "UP".equals(item.estado())));
    }

    private DocumentoElectronico documentoConEstado(DocumentoEstado estado) {
        DocumentoElectronico documento = new DocumentoElectronico();
        documento.setEstadoActual(estado);
        return documento;
    }
}
