package com.erp.sri_files.service;

import com.erp.sri_files.domain.documento.DocumentoElectronico;
import com.erp.sri_files.domain.documento.DocumentoEstado;
import com.erp.sri_files.exceptions.DocumentoRecepcionException;
import com.erp.sri_files.repositories.documento.DocumentoElectronicoRepository;
import com.erp.sri_files.repositories.documento.DocumentoEstadoHistorialRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class EstadoDocumentoServiceTest {

    private DocumentoElectronicoRepository documentoRepository;
    private DocumentoEstadoHistorialRepository historialRepository;
    private EstadoDocumentoService service;

    @BeforeEach
    void setUp() {
        documentoRepository = mock(DocumentoElectronicoRepository.class);
        historialRepository = mock(DocumentoEstadoHistorialRepository.class);
        service = new EstadoDocumentoService(documentoRepository, historialRepository);
        when(documentoRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void cambiaEstadoYGuardaHistorial() {
        DocumentoElectronico documento = new DocumentoElectronico();
        documento.setEstadoActual(DocumentoEstado.RECIBIDO);

        DocumentoElectronico actualizado = service.cambiar(documento, DocumentoEstado.VALIDANDO, "Paso a validacion");

        assertEquals(DocumentoEstado.VALIDANDO, actualizado.getEstadoActual());
        verify(historialRepository).save(any());
    }

    @Test
    void rechazaTransicionInvalida() {
        DocumentoElectronico documento = new DocumentoElectronico();
        documento.setEstadoActual(DocumentoEstado.RECIBIDO);

        assertThrows(DocumentoRecepcionException.class,
                () -> service.cambiar(documento, DocumentoEstado.AUTORIZADO, "Salto invalido"));
    }
}
