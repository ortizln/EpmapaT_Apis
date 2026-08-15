package com.erp.sri_files.service;

import com.erp.sri_files.domain.documento.DocumentoElectronico;
import com.erp.sri_files.domain.documento.DocumentoEstado;
import com.erp.sri_files.domain.documento.Empresa;
import com.erp.sri_files.dto.request.DocumentoRecepcionRequest;
import com.erp.sri_files.dto.response.DocumentoRecepcionResponse;
import com.erp.sri_files.exceptions.DocumentoRecepcionException;
import com.erp.sri_files.repositories.documento.DocumentoElectronicoRepository;
import com.erp.sri_files.repositories.documento.DocumentoEstadoHistorialRepository;
import com.erp.sri_files.repositories.documento.EmpresaRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DocumentoRecepcionServiceTest {

    private DocumentoElectronicoRepository documentoRepository;
    private DocumentoEstadoHistorialRepository historialRepository;
    private EmpresaRepository empresaRepository;
    private ArchivoDocumentoService archivoDocumentoService;
    private DocumentoRecepcionService service;

    @BeforeEach
    void setUp() {
        documentoRepository = mock(DocumentoElectronicoRepository.class);
        historialRepository = mock(DocumentoEstadoHistorialRepository.class);
        empresaRepository = mock(EmpresaRepository.class);
        archivoDocumentoService = mock(ArchivoDocumentoService.class);
        service = new DocumentoRecepcionService(
                documentoRepository,
                historialRepository,
                empresaRepository,
                new ObjectMapper(),
                archivoDocumentoService
        );
    }

    @Test
    void guardaDocumentoNuevoYCreaHistorial() {
        Empresa empresa = new Empresa();
        empresa.setUuid(UUID.randomUUID());
        empresa.setRuc("1790012345001");
        empresa.setRazonSocial("EPMAPA");
        empresa.setSriAmbiente((short) 2);

        when(empresaRepository.findByRuc("1790012345001")).thenReturn(Optional.of(empresa));
        when(documentoRepository.findByEmpresaIdAndExternalId(isNull(), anyString())).thenReturn(Optional.empty());
        when(documentoRepository.findByEmpresaIdAndIdempotencyKey(isNull(), anyString())).thenReturn(Optional.empty());
        when(documentoRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        DocumentoRecepcionResponse response = service.recibir(requestBase(), "idem-1");

        assertEquals("FACTURA", response.tipoDocumento());
        assertEquals(DocumentoEstado.RECIBIDO.name(), response.estado());
        assertFalse(response.duplicado());

        ArgumentCaptor<DocumentoElectronico> captor = ArgumentCaptor.forClass(DocumentoElectronico.class);
        verify(documentoRepository).save(captor.capture());
        DocumentoElectronico saved = captor.getValue();
        assertEquals(LocalDate.parse("2026-08-14"), saved.getFechaEmision());
        assertEquals("001-002-000000123", saved.getNumeroDocumento());
        assertEquals((short) 1, saved.getAmbiente());
        assertNotNull(saved.getJsonOriginal());

        verify(historialRepository).save(any());
        verify(archivoDocumentoService).guardarJsonOriginal(saved);
    }

    @Test
    void retornaDuplicadoSiExternalIdExiste() {
        Empresa empresa = new Empresa();
        empresa.setUuid(UUID.randomUUID());
        empresa.setRuc("1790012345001");
        empresa.setRazonSocial("EPMAPA");
        empresa.setSriAmbiente((short) 2);

        DocumentoElectronico existente = new DocumentoElectronico();
        existente.setUuid(UUID.randomUUID());
        existente.setTipoDocumento(com.erp.sri_files.domain.documento.TipoDocumento.FACTURA);
        existente.setEstadoActual(DocumentoEstado.AUTORIZADO);

        when(empresaRepository.findByRuc("1790012345001")).thenReturn(Optional.of(empresa));
        when(documentoRepository.findByEmpresaIdAndExternalId(isNull(), anyString())).thenReturn(Optional.of(existente));

        DocumentoRecepcionResponse response = service.recibir(requestBase(), "idem-1");

        assertEquals("AUTORIZADO", response.estado());
        assertEquals(true, response.duplicado());
        verify(documentoRepository, never()).save(any());
    }

    @Test
    void fallaSiNoExisteEmpresaConfigurada() {
        when(empresaRepository.findByRuc("1790012345001")).thenReturn(Optional.empty());

        assertThrows(DocumentoRecepcionException.class, () -> service.recibir(requestBase(), "idem-1"));
    }

    @Test
    void usaAmbienteDeEmpresaSiElPayloadNoLoEnvia() {
        Empresa empresa = new Empresa();
        empresa.setUuid(UUID.randomUUID());
        empresa.setRuc("1790012345001");
        empresa.setRazonSocial("EPMAPA");
        empresa.setSriAmbiente((short) 2);

        when(empresaRepository.findByRuc("1790012345001")).thenReturn(Optional.of(empresa));
        when(documentoRepository.findByEmpresaIdAndExternalId(isNull(), anyString())).thenReturn(Optional.empty());
        when(documentoRepository.findByEmpresaIdAndIdempotencyKey(isNull(), anyString())).thenReturn(Optional.empty());
        when(documentoRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        service.recibir(requestSinAmbiente(), "idem-2");

        ArgumentCaptor<DocumentoElectronico> captor = ArgumentCaptor.forClass(DocumentoElectronico.class);
        verify(documentoRepository).save(captor.capture());
        assertEquals((short) 2, captor.getValue().getAmbiente());
    }

    private DocumentoRecepcionRequest requestBase() {
        return new DocumentoRecepcionRequest(
                "FACTURA",
                "ERP-001",
                Map.of("ruc", "1790012345001", "establecimiento", "001", "puntoEmision", "002", "ambiente", "1"),
                Map.of("identificacion", "0102030405", "razonSocial", "Cliente Demo", "email", "cliente@correo.com"),
                Map.of("fechaEmision", "2026-08-14", "secuencial", "000000123", "subtotal", "10.00", "impuestos", "1.20", "total", "11.20", "moneda", "USD"),
                List.of(),
                List.of(),
                Map.of(),
                new DocumentoRecepcionRequest.CorreoRequest(true, List.of("cliente@correo.com"))
        );
    }

    private DocumentoRecepcionRequest requestSinAmbiente() {
        return new DocumentoRecepcionRequest(
                "FACTURA",
                "ERP-002",
                Map.of("ruc", "1790012345001", "establecimiento", "001", "puntoEmision", "002"),
                Map.of("identificacion", "0102030405", "razonSocial", "Cliente Demo", "email", "cliente@correo.com"),
                Map.of("fechaEmision", "2026-08-14", "secuencial", "000000124", "subtotal", "10.00", "impuestos", "1.20", "total", "11.20", "moneda", "USD"),
                List.of(),
                List.of(),
                Map.of(),
                new DocumentoRecepcionRequest.CorreoRequest(true, List.of("cliente@correo.com"))
        );
    }
}
