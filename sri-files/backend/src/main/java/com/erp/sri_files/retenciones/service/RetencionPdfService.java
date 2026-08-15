package com.erp.sri_files.retenciones.service;

import com.erp.sri_files.retenciones.utils.SriRetencionParser;
import net.sf.jasperreports.engine.*;
import net.sf.jasperreports.engine.data.JRBeanCollectionDataSource;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.net.URL;
import java.util.Map;

@Service
public class RetencionPdfService {

    public byte[] generarPdfDesdeXmlAutorizado(String xmlAutorizacion) throws Exception {
        var data = SriRetencionParser.parseAutorizacion(xmlAutorizacion);
        URL logoUrl = new ClassPathResource("reports/LOGO-H.png").getURL();
        data.params.put("logoUrl", logoUrl);

        JRBeanCollectionDataSource impuestosDs = new JRBeanCollectionDataSource(data.impuestos);

        InputStream jrxml = new ClassPathResource("reports/retencion_template.jrxml")
                .getInputStream();

        JasperReport report = JasperCompileManager.compileReport(jrxml);

        JasperPrint print = JasperFillManager.fillReport(
                report,
                (Map<String, Object>) data.params,
                impuestosDs // ✅ AQUÍ VA LA LISTA
        );

        return JasperExportManager.exportReportToPdf(print);
    }
}
