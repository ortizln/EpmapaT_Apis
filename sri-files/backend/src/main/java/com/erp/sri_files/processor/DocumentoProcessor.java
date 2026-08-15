package com.erp.sri_files.processor;

import com.erp.sri_files.domain.documento.DocumentoElectronico;
import com.erp.sri_files.domain.documento.TipoDocumento;

public interface DocumentoProcessor {

    TipoDocumento soporta();

    void validar(DocumentoElectronico documento);
}
