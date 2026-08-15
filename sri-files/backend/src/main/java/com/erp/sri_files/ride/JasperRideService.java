package com.erp.sri_files.ride;

import com.erp.sri_files.services.XmlToPdfService;
import org.springframework.stereotype.Service;

@Service
public class JasperRideService implements RideService {

    private final XmlToPdfService xmlToPdfService;

    public JasperRideService(XmlToPdfService xmlToPdfService) {
        this.xmlToPdfService = xmlToPdfService;
    }

    @Override
    public byte[] generar(String xmlAutorizado) throws Exception {
        return xmlToPdfService.generarFacturaPDF_v3(xmlAutorizado).toByteArray();
    }
}
