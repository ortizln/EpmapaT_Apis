package com.erp.sri_files.signature;

import com.erp.sri_files.config.AESUtil;
import com.erp.sri_files.domain.documento.DocumentoElectronico;
import com.erp.sri_files.domain.documento.Empresa;
import com.erp.sri_files.utils.FirmaComprobantesService;
import com.erp.sri_files.utils.FirmaComprobantesService.ModoFirma;
import com.erp.sri_files.utils.Pkcs12Loader;
import com.erp.sri_files.utils.XadesBesService;
import org.springframework.stereotype.Service;

@Service
public class LegacyFirmaElectronicaService implements FirmaElectronicaService {

    private final FirmaComprobantesService firmaComprobantesService;
    private final XadesBesService xadesBesService;

    public LegacyFirmaElectronicaService(
            FirmaComprobantesService firmaComprobantesService,
            XadesBesService xadesBesService
    ) {
        this.firmaComprobantesService = firmaComprobantesService;
        this.xadesBesService = xadesBesService;
    }

    @Override
    public String firmar(DocumentoElectronico documento, String xml) {
        try {
            Empresa empresa = documento == null ? null : documento.getEmpresa();
            if (empresa != null && empresa.getCertificadoPkcs12() != null && empresa.getCertificadoPkcs12().length > 0) {
                String clave = empresa.getCertificadoClave() == null ? "" : AESUtil.descifrar(empresa.getCertificadoClave());
                Pkcs12Loader.KeyMaterial keyMaterial = Pkcs12Loader.load(
                        empresa.getCertificadoPkcs12(),
                        clave.toCharArray()
                );
                return xadesBesService.signComprobanteXades(
                        xml.getBytes(java.nio.charset.StandardCharsets.UTF_8),
                        keyMaterial
                );
            }
            return firmaComprobantesService.firmarFactura(xml, ModoFirma.XADES_BES);
        } catch (Exception ex) {
            throw new IllegalStateException("No fue posible firmar electronicamente el documento", ex);
        }
    }
}
