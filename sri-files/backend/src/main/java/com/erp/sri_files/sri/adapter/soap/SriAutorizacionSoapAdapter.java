package com.erp.sri_files.sri.adapter.soap;

import com.erp.sri_files.domain.documento.DocumentoElectronico;
import com.erp.sri_files.services.SendXmlToSriService;
import com.erp.sri_files.sri.port.SriAutorizacionPort;
import ec.gob.sri.ws.autorizacion.RespuestaComprobante;
import org.springframework.stereotype.Component;

@Component
public class SriAutorizacionSoapAdapter implements SriAutorizacionPort {

    private final SendXmlToSriService sendXmlToSriService;

    public SriAutorizacionSoapAdapter(SendXmlToSriService sendXmlToSriService) {
        this.sendXmlToSriService = sendXmlToSriService;
    }

    @Override
    public RespuestaComprobante consultar(String claveAcceso) throws Exception {
        return sendXmlToSriService.consultarAutorizacion(claveAcceso);
    }

    @Override
    public RespuestaComprobante consultar(DocumentoElectronico documento, String claveAcceso) throws Exception {
        int ambiente = documento == null ? sendXmlToSriService.getAmbiente() : documento.getAmbiente();
        return sendXmlToSriService.consultarAutorizacion(claveAcceso, ambiente);
    }
}
