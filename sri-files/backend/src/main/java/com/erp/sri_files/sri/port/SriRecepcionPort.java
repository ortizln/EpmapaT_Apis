package com.erp.sri_files.sri.port;

import com.erp.sri_files.domain.documento.DocumentoElectronico;
import ec.gob.sri.ws.recepcion.RespuestaSolicitud;

public interface SriRecepcionPort {

    RespuestaSolicitud enviar(DocumentoElectronico documento, String xmlFirmado) throws Exception;
}
