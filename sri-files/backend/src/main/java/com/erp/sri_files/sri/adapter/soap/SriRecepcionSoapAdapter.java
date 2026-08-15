package com.erp.sri_files.sri.adapter.soap;

import com.erp.sri_files.domain.documento.DocumentoElectronico;
import com.erp.sri_files.services.SendXmlToSriService;
import com.erp.sri_files.sri.port.SriRecepcionPort;
import ec.gob.sri.ws.recepcion.RespuestaSolicitud;
import org.springframework.stereotype.Component;

@Component
public class SriRecepcionSoapAdapter implements SriRecepcionPort {

    private final SendXmlToSriService sendXmlToSriService;

    public SriRecepcionSoapAdapter(SendXmlToSriService sendXmlToSriService) {
        this.sendXmlToSriService = sendXmlToSriService;
    }

    @Override
    public RespuestaSolicitud enviar(DocumentoElectronico documento, String xmlFirmado) throws Exception {
        int ambiente = documento == null ? sendXmlToSriService.getAmbiente() : documento.getAmbiente();
        return sendXmlToSriService.enviarFacturaFirmadaTxt(xmlFirmado, ambiente);
    }
}
