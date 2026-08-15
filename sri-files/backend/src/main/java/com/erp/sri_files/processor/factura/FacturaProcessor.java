package com.erp.sri_files.processor.factura;

import com.erp.sri_files.domain.documento.DocumentoElectronico;
import com.erp.sri_files.domain.documento.TipoDocumento;
import com.erp.sri_files.exceptions.DocumentoRecepcionException;
import com.erp.sri_files.processor.DocumentoProcessor;
import org.springframework.stereotype.Component;

@Component
public class FacturaProcessor implements DocumentoProcessor {

    @Override
    public TipoDocumento soporta() {
        return TipoDocumento.FACTURA;
    }

    @Override
    public void validar(DocumentoElectronico documento) {
        if (documento.getFechaEmision() == null) {
            throw new DocumentoRecepcionException("La factura no tiene fecha de emision");
        }
        if (documento.getIdentificacionReceptor() == null || documento.getIdentificacionReceptor().isBlank()) {
            throw new DocumentoRecepcionException("La factura no tiene identificacion del receptor");
        }
        if (documento.getTotal() == null) {
            throw new DocumentoRecepcionException("La factura no tiene total");
        }
    }
}
