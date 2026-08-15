package com.erp.sri_files.signature;

import com.erp.sri_files.domain.documento.DocumentoElectronico;

public interface FirmaElectronicaService {

    String firmar(DocumentoElectronico documento, String xml);
}
