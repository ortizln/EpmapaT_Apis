package com.erp.sri_files.utils;

import com.erp.sri_files.config.AESUtil;
import com.erp.sri_files.models.Definir;
import com.erp.sri_files.repositories.DefinirR;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;

@Service
public class FirmaComprobantesService {

    public enum ModoFirma { XMLDSIG, XADES_BES }

    private final DefinirR certRepo;
    private final XmlDsigService xmlDsigService;
    private final XadesBesService xadesBesService;

    public FirmaComprobantesService(DefinirR certRepo,
                                    XmlDsigService xmlDsigService,
                                    XadesBesService xadesBesService) {
        this.certRepo = certRepo;
        this.xmlDsigService = xmlDsigService;
        this.xadesBesService = xadesBesService;
    }

    // Firmar desde String
    @Transactional
    public String firmarFactura(String xmlPlano, ModoFirma modo) throws Exception {
        Definir cert = certRepo.findById(1L)
                .orElseThrow(() -> new IllegalStateException("Certificado no encontrado id=" ));

        byte[] pkcs12 = cert.getFirma();
        if (pkcs12 == null || pkcs12.length == 0)
            throw new IllegalStateException("El campo DEFINIR.firma está vacío");

        String password = AESUtil.descifrar(cert.getClave_firma());
        var km = Pkcs12Loader.load(pkcs12, password !=null? password.toCharArray(): new char[0]);

        return switch (modo) {
            case XMLDSIG -> xmlDsigService.signFacturaComprobante(xmlPlano, km);
            case XADES_BES -> xadesBesService.signComprobanteXades(xmlPlano.getBytes(StandardCharsets.UTF_8), km);
        };
    }

    // Firmar desde byte[] (más robusto contra BOM / encoding)
    @Transactional
    public String firmarFactura(byte[] xmlBytes, ModoFirma modo) throws Exception {
        Definir cert = certRepo.findById(1L)
                .orElseThrow(() -> new IllegalStateException("Certificado no encontrado id="));

        byte[] pkcs12 = cert.getFirma();
        if (pkcs12 == null || pkcs12.length == 0)
            throw new IllegalStateException("El campo DEFINIR.firma está vacío");
        String password = AESUtil.descifrar(cert.getClave_firma());
        var km = Pkcs12Loader.load(pkcs12, password !=null? password.toCharArray(): new char[0]);

        return switch (modo) {
            case XMLDSIG -> xmlDsigService.signFacturaComprobante(
                    new String(xmlBytes, StandardCharsets.UTF_8), km
            );
            case XADES_BES -> xadesBesService.signComprobanteXades(xmlBytes, km);
        };
    }
}
