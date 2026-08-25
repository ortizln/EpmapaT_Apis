package com.erp.sri_files.service;

import com.erp.sri_files.domain.documento.DocumentoElectronico;
import com.erp.sri_files.domain.documento.DocumentoEstado;
import com.erp.sri_files.domain.documento.DocumentoEstadoHistorial;
import com.erp.sri_files.domain.documento.TipoDocumento;
import com.erp.sri_files.dto.response.DocumentoAuditoriaResumenResponse;
import com.erp.sri_files.repositories.documento.DocumentoElectronicoRepository;
import com.erp.sri_files.repositories.documento.DocumentoErrorRepository;
import com.erp.sri_files.repositories.documento.DocumentoEstadoHistorialRepository;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class DocumentoAuditoriaServiceTest {

    @Test
    void obtieneAuditoriaRecienteDesdeHistorial() {
        DocumentoElectronicoRepository documentoRepository = mock(DocumentoElectronicoRepository.class);
        DocumentoEstadoHistorialRepository historialRepository = mock(DocumentoEstadoHistorialRepository.class);
        DocumentoErrorRepository errorRepository = mock(DocumentoErrorRepository.class);
        ArchivoDocumentoService archivoDocumentoService = mock(ArchivoDocumentoService.class);

        DocumentoConsultaService service = new DocumentoConsultaService(
                documentoRepository,
                historialRepository,
                errorRepository,
                archivoDocumentoService
        );

        DocumentoElectronico documento = new DocumentoElectronico();
        documento.setUuid(UUID.randomUUID());
        documento.setTipoDocumento(TipoDocumento.FACTURA);
        documento.setNumeroDocumento("001-001-000000123");
        documento.setExternalId("ERP-001");

        DocumentoEstadoHistorial historial = new DocumentoEstadoHistorial();
        historial.setDocumento(documento);
        historial.setEstadoAnterior(DocumentoEstado.RECIBIDO);
        historial.setEstadoNuevo(DocumentoEstado.XML_GENERADO);
        historial.setDescripcion("XML del documento generado");
        historial.setOrigen("SYSTEM");
        historial.setCreatedAt(LocalDateTime.of(2026, 8, 15, 18, 30));

        when(historialRepository.findTop100ByOrderByCreatedAtDesc()).thenReturn(List.of(historial));

        DocumentoAuditoriaResumenResponse response = service.obtenerAuditoriaReciente();

        assertEquals(1, response.totalEventos());
        assertEquals("FACTURA", response.eventos().get(0).tipoDocumento());
        assertEquals("XML_GENERADO", response.eventos().get(0).estadoNuevo());
        assertEquals("ERP-001", response.eventos().get(0).externalId());
    }
}
