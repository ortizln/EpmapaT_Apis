package com.erp.sri_files.sri.port;

import com.erp.sri_files.domain.documento.DocumentoElectronico;
import ec.gob.sri.ws.autorizacion.RespuestaComprobante;

public interface SriAutorizacionPort {

    RespuestaComprobante consultar(String claveAcceso) throws Exception;

    RespuestaComprobante consultar(DocumentoElectronico documento, String claveAcceso) throws Exception;
}
