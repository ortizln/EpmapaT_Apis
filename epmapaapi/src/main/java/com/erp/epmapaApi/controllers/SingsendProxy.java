package com.erp.epmapaApi.controllers;

import com.erp.epmapaApi.models.sri.FecFactura;
import com.erp.epmapaApi.repositories.sri.FecFacturaR;
import com.erp.epmapaApi.services.SriPdfService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.Map;

@RestController
@RequestMapping("/api/singsend")
@RequiredArgsConstructor
@Slf4j
public class SingsendProxy {

    private final FecFacturaR fecFacturaR;
    private final SriPdfService sriPdfService;

    @GetMapping("/generar-pdf")
    public ResponseEntity<?> generarPdf(@RequestParam Long idfactura) {
        try {
            FecFactura factura = fecFacturaR.findByIdfactura(idfactura);
            if (factura == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("codigo", "FACTURA_NO_ENCONTRADA", "error", "No se encontró la factura", "idfactura", idfactura));
            }

            String xmlAutorizado = factura.getXmlautorizado();
            if (xmlAutorizado == null || xmlAutorizado.isBlank()) {
                return ResponseEntity.status(HttpStatus.CONFLICT)
                        .body(Map.of("codigo", "XML_AUTORIZADO_NO_ENCONTRADO", "error", "La factura aún no cuenta con XML autorizado", "idfactura", idfactura));
            }

            ByteArrayOutputStream pdfStream = sriPdfService.generarFacturaPDF(xmlAutorizado, factura.getReferencia(), factura.getGuiaremision());

            if (pdfStream == null || pdfStream.size() == 0) {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body(Map.of("codigo", "PDF_VACIO", "error", "No se pudo generar el PDF", "idfactura", idfactura));
            }

            InputStreamResource resource = new InputStreamResource(new ByteArrayInputStream(pdfStream.toByteArray()));
            String nombreArchivo = "factura_" + idfactura + ".pdf";

            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + nombreArchivo)
                    .contentType(MediaType.APPLICATION_PDF)
                    .contentLength(pdfStream.size())
                    .body(resource);

        } catch (Exception e) {
            log.error("=== PDF ERROR === idfactura={}", idfactura, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("codigo", "ERROR_GENERANDO_PDF", "error", "Error generando el PDF", "detalle", e.getMessage(), "idfactura", idfactura));
        }
    }
}
