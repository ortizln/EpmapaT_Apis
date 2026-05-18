package com.erp.sri_files.services;

import com.erp.sri_files.models.YourDataModel;
import net.sf.jasperreports.engine.JRException;
import net.sf.jasperreports.engine.JasperCompileManager;
import net.sf.jasperreports.engine.JasperExportManager;
import net.sf.jasperreports.engine.JasperFillManager;
import net.sf.jasperreports.engine.JasperPrint;
import net.sf.jasperreports.engine.JasperReport;
import net.sf.jasperreports.engine.data.JRBeanCollectionDataSource;

import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

@Service
public class PdfGenerationService {

  public byte[] generatePdfFromData(YourDataModel data) throws JRException, IOException {
        // Cargar el reporte compilado (.jasper) o compilar el .jrxml
        InputStream reportStream = new ClassPathResource("reports/report_template.jrxml").getInputStream();
        JasperReport jasperReport = JasperCompileManager.compileReport(reportStream);
        
        // Preparar los parámetros
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("param1", data.getField1());
        parameters.put("param2", data.getField2());
        
        // Crear datasource
        JRBeanCollectionDataSource dataSource = new JRBeanCollectionDataSource(Collections.singletonList(data));
        
        // Generar el reporte
        JasperPrint jasperPrint = JasperFillManager.fillReport(
            jasperReport, 
            parameters, 
            dataSource
        );
        
        // Exportar a PDF
        return JasperExportManager.exportReportToPdf(jasperPrint);
    }

/*     public static byte[] generatePdfFromXml(byte[] xmlBytes) {
        try {
            System.out.println("Paso 1: Parseando el XML...");
            Document document = XmlParser.parseXmlFromBytes(xmlBytes);
            System.out.println("Paso 2: Extrayendo información de la factura...");
            Map<String, String> facturaData = EcuadorianInvoiceExtractor.extractData(document);
            System.out.println("Datos extraídos: " + facturaData);

            System.out.println("Paso 3: Validando datos...");
            InvoiceValidator.validate(facturaData);

            System.out.println("Paso 4: Generando PDF...");
            return PdfGenerator.generate(facturaData);
        } catch (XmlParser.XmlParseException e) {
            System.err.println("Error al parsear el XML: " + e.getMessage());
        } catch (IllegalArgumentException e) {
            System.err.println("Error de validación: " + e.getMessage());
        } catch (IOException e) {
            System.err.println("Error al generar el PDF: " + e.getMessage());
        }
        return null;
    } */
    
/*         public void generarReporte(String urlReporte, FacturaReporte fact, String numAut, String fechaAut) throws SQLException, ClassNotFoundException {
        FileInputStream is = null;
        try {
            //String dirAutorizados = "/home/odoo/comprobantes/autorizados/";
            String dirAutorizados = "C:\\comprobantes\\autorizados\\";
            JRBeanCollectionDataSource jRBeanCollectionDataSource = new JRBeanCollectionDataSource(fact.getDetallesAdiciones());
            is = new FileInputStream(urlReporte);
            JasperPrint print = JasperFillManager.fillReport(is, obtenerMapaParametrosReportes(obtenerParametrosInfoTriobutaria(fact.getFacturaXML().getInfoTributaria(), numAut, fechaAut), obtenerInfoFactura(fact.getFacturaXML().getInfoFactura(), fact)), (JRDataSource) jRBeanCollectionDataSource);
            // Transformar a PDF
            JasperExportManager.exportReportToPdfFile(print, dirAutorizados + fact.getFacturaXML().getInfoTributaria().getClaveAcceso() + ".pdf");
        } catch (FileNotFoundException | JRException ex) {
            Logger.getLogger(ReporteUtil.class.getName()).log(Level.FATAL, null, ex);
            //ex.printStackTrace();
        } finally {
            try {
                if (is != null) {
                    is.close();
                }
            } catch (IOException ex) {
                Logger.getLogger(ReporteUtil.class.getName()).log(Level.FATAL, null, ex);
                //ex.printStackTrace();
            }
        }
    }
 */

}