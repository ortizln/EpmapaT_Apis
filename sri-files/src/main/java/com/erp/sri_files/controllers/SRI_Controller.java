package com.erp.sri_files.controllers;

import com.erp.sri_files.dto.*;
import com.erp.sri_files.models.Factura;
import com.erp.sri_files.repositories.DefinirR;
import com.erp.sri_files.repositories.FacturaR;
import com.erp.sri_files.retenciones.service.RetencionEmailService;
import com.erp.sri_files.retenciones.service.RetencionPdfService;
import com.erp.sri_files.services.*;
import com.erp.sri_files.utils.FirmaComprobantesService;
import com.erp.sri_files.utils.FirmaComprobantesService.ModoFirma;
import com.erp.sri_files.validation.SriRetencionValidationService;
import ec.gob.sri.ws.autorizacion.RespuestaComprobante;
import ec.gob.sri.ws.recepcion.RespuestaSolicitud;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/singsend")
public class SRI_Controller {

    private static final Logger log = LoggerFactory.getLogger(SRI_Controller.class);

    private final SendXmlToSriService sendXmlToSriService;
    private final FirmaComprobantesService firmaService;
    private final RestTemplate restTemplate;
    private final FacturaR fecFacturaR;
    private final FacturaXmlGeneratorService facturaXmlGeneratorService;
    private final FacturasService facturasService;
    private final XmlToPdfService xmlToPdfService;
    private final DefinirR definirService;
    private final MailService mailService;
    private final RetencionPdfService   retencionPdfService;
    private final RetencionEmailService retencionEmailService;
    private final SriRetencionValidationService retencionValidationService;


    @Value("${app.backend.base-url:http://192.168.0.165:9080}")
    private String backendBaseUrl;


    // ===== Helpers / DTOs =====
    public record FirmarResp(String xmlFirmado) {}

    private static String toUtf8String(MultipartFile file) throws Exception {
        if (file == null || file.isEmpty())
            throw new IllegalArgumentException("Archivo XML vacio o no enviado (parte 'xml').");
        String s = new String(file.getBytes(), StandardCharsets.UTF_8);
        // elimina BOM si existe (importante para firmas)
        if (!s.isEmpty() && s.charAt(0) == '\uFEFF') s = s.substring(1);
        return s;
    }

    private static String extraerTag(String xml, String tagName) throws Exception {
        var dbf = DocumentBuilderFactory.newInstance();
        dbf.setNamespaceAware(true);
        var doc = dbf.newDocumentBuilder()
                .parse(new org.xml.sax.InputSource(new java.io.StringReader(xml)));
        var list = doc.getElementsByTagName(tagName);
        if (list.getLength() == 0) return null;
        return list.item(0).getTextContent().replaceAll("\\s+", "");
    }

    /*
     * ========================================================================================================
                      * FACTURAS
     * ========================================================================================================
     */


    // ===================== 1) FACTURA FIRMAR Y ENVIAR EN UNO SOLO XML-FILE =====================
    @PostMapping(
            path = "/factura/xml"
    )
    public ResponseEntity<?> firmarYEnviarFacturaUloadXml(
            @RequestPart("xml") MultipartFile xmlFile,
            @RequestParam(value = "modo", required = false, defaultValue = "XADES_BES") String modo,
            @RequestParam(value = "definirId", required = false, defaultValue = "1") Long definirId,
            @RequestParam(value = "ambiente", required = false) Integer ambienteForzado
    ) {
        try {
            // 1) Convertir el archivo a String
            String xmlPlano = toUtf8String(xmlFile);
            // 2) Elegir modo de firma
            ModoFirma mf = "XMLDSIG".equalsIgnoreCase(modo) ? ModoFirma.XMLDSIG : ModoFirma.XADES_BES;
            // 3) Firmar
            String xmlFirmado = (definirId == null)
                    ? firmaService.firmarFactura(xmlPlano, mf)
                    : firmaService.firmarFactura(xmlPlano, mf);

            // 4) Ambiente: forzado si lo pasan; si no, deducir del XML firmado
            if (ambienteForzado != null) {
                sendXmlToSriService.setAmbiente(ambienteForzado == 2 ? 2 : 1);
            } else {
                sendXmlToSriService.setAmbienteFromXml(xmlFirmado);
            }

            // 5) Enviar a recepciÃƒÆ’Ã‚Â³n
            RespuestaSolicitud recepcion = sendXmlToSriService.enviarFacturaFirmadaTxt(xmlFirmado);

            // 6) Si RECIBIDA, aplicar polling hasta autorizacion
            if ("RECIBIDA".equalsIgnoreCase(recepcion.getEstado())) {
                RespuestaComprobante autorizacion = sendXmlToSriService.consultarAutorizacionConEspera(
                        xmlFirmado,
                        clave -> {
                            try { return sendXmlToSriService.consultarAutorizacion(clave); }
                            catch (Exception e) { throw new RuntimeException(e); }
                        },
                        10,   // intentos
                        4000  // ms
                );
                return ResponseEntity.ok(Map.of(
                        "autorizacion", autorizacion
                ));
            } else {
                return ResponseEntity.badRequest().body(Map.of(
                        "recepcion", recepcion,
                        "xmlFirmado", xmlFirmado
                ));
            }
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body(Map.of(
                    "error", "Error al firmar/enviar comprobante",
                    "detalle", e.getMessage()
            ));
        }
    }

    // ===================== 2)  FACTURA FIRMAR Y ENVIAR EN UNO SOLO XML-STRING =====================
    @PostMapping(
            path = "/factura/string"
    )
    public ResponseEntity<?> firmarYEnviarFacturaXml(
            @RequestBody String xmlPlano,
            @RequestParam(value = "modo", required = false, defaultValue = "XADES_BES") String modo,
            @RequestParam(value = "definirId", required = false, defaultValue = "1") Long definirId,
            @RequestParam(value = "ambiente", required = false) Integer ambienteForzado
            // @RequestParam(value = "password", required = false) String password
    ) {
        try {
            // Limpia BOM si existe (muy importante para evitar invalidar la firma)
            if (!xmlPlano.isEmpty() && xmlPlano.charAt(0) == '\uFEFF') {
                xmlPlano = xmlPlano.substring(1);
            }

            // 1) Elegir modo de firma
            ModoFirma mf = "XMLDSIG".equalsIgnoreCase(modo) ? ModoFirma.XMLDSIG : ModoFirma.XADES_BES;

            // 2) Firmar
            String xmlFirmado = (definirId == null)
                    ? firmaService.firmarFactura(xmlPlano, mf)
                    : firmaService.firmarFactura(xmlPlano, mf);

            // 3) Ambiente: forzado si lo pasan; si no, deducir del XML firmado
            if (ambienteForzado != null) {
                sendXmlToSriService.setAmbiente(ambienteForzado == 2 ? 2 : 1);
            } else {
                sendXmlToSriService.setAmbienteFromXml(xmlFirmado);
            }

            // 4) Enviar a recepciÃƒÆ’Ã‚Â³n
            RespuestaSolicitud recepcion = sendXmlToSriService.enviarFacturaFirmadaTxt(xmlFirmado);


            // 5) Si RECIBIDA, aplicar polling hasta autorizacion
            if ("RECIBIDA".equalsIgnoreCase(recepcion.getEstado())) {
                RespuestaComprobante autorizacion = sendXmlToSriService.consultarAutorizacionConEspera(
                        xmlFirmado,
                        clave -> {
                            try { return sendXmlToSriService.consultarAutorizacion(clave); }
                            catch (Exception e) { throw new RuntimeException(e); }
                        },
                        10,   // intentos
                        4000  // ms
                );
                return ResponseEntity.ok(Map.of(
                        "autorizacion", autorizacion
                ));
            } else {
                return ResponseEntity.badRequest().body(Map.of(
                        "recepcion", recepcion,
                        "xmlFirmado", xmlFirmado
                ));
            }
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body(Map.of(
                    "error", "Error al firmar/enviar comprobante",
                    "detalle", e.getMessage()
            ));
        }
    }

    // ===================== 3) FIRMAR Y ENVIAR RECIBIENDO XML EN STRING =====================
    @PostMapping(
            path = "/factura"
    )
    public ResponseEntity<?> firmarYEnviarFactura(
            @RequestBody String xmlPlano,
            @RequestParam(value = "modo", required = false, defaultValue = "XADES_BES") String modo,
            @RequestParam(value = "definirId", required = false, defaultValue = "1") Long definirId,
            @RequestParam(value = "ambiente", required = false) Integer ambienteForzado
    ) {
        try {
            // Limpia BOM si existe (importante para parseo/firma)
            if (xmlPlano != null && !xmlPlano.isEmpty() && xmlPlano.charAt(0) == '\uFEFF') {
                xmlPlano = xmlPlano.substring(1);
            }

            // 1) Elegir modo de firma
            ModoFirma mf = "XMLDSIG".equalsIgnoreCase(modo) ? ModoFirma.XMLDSIG : ModoFirma.XADES_BES;

            // 2) Firmar (usa definirId correcto)
            String xmlFirmado = firmaService.firmarFactura(xmlPlano, mf);

            // 2) Ambiente
            if (ambienteForzado != null) {
                sendXmlToSriService.setAmbiente(ambienteForzado == 2 ? 2 : 1);
            } else {
                sendXmlToSriService.setAmbienteFromXml(xmlFirmado);
            }

            // 4) Enviar a recepciÃƒÆ’Ã‚Â³n
            RespuestaSolicitud recepcion = sendXmlToSriService.enviarFacturaFirmadaTxt(xmlFirmado);

            // Si NO fue recibida, devolvemos error resumido (JSON)
            if (!"RECIBIDA".equalsIgnoreCase(recepcion.getEstado())) {
                return ResponseEntity.badRequest().body(Map.of(
                        "estado", recepcion.getEstado(),
                        "errores", resumenErroresRecepcion(recepcion)
                ));
            }

            // 5) Polling/consulta de autorizacion
            RespuestaComprobante rc = sendXmlToSriService.consultarAutorizacionConEspera(
                    xmlFirmado,
                    clave -> {
                        try { return sendXmlToSriService.consultarAutorizacion(clave); }
                        catch (Exception e) { throw new RuntimeException(e); }
                    },
                    10,   // intentos
                    4000  // ms entre intentos
            );

            // 6) Extraer el XML autorizado (si existe)
            String xmlAutorizado = extraerXmlAutorizado(rc);
            if (xmlAutorizado != null) {
                // ÃƒÂ¢Ã…â€œÃ¢â‚¬Â¦ SOLO devolvemos el XML autorizado
                return ResponseEntity.ok()
                        .contentType(MediaType.APPLICATION_XML)
                        .body(xmlAutorizado);
            }

            // Si no hay autorizacion aÃƒÆ’Ã‚Âºn, devuelve 202 con mensaje breve
            return ResponseEntity.status(202).body(Map.of(
                    "estado", "SIN_AUTORIZACION_EN_SRI",
                    "detalle", "La autorizacion aÃƒÆ’Ã‚Âºn no estÃƒÆ’Ã‚Â¡ disponible."
            ));

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body(Map.of(
                    "error", "Error al firmar/enviar comprobante",
                    "detalle", e.getMessage()
            ));
        }
    }

    /*
     * GENERAR PDF de FACTURA
     * */

    @GetMapping("/generar-pdf")
    public ResponseEntity<?> generarPdf(@RequestParam Long idfactura) {
        try {
            // 1) Traer la factura del microservicio/repositorio
            Factura factura = fecFacturaR.findByIdfactura(idfactura);

            if (factura == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of(
                                "codigo", "FACTURA_NO_ENCONTRADA",
                                "error", "No se encontrÃƒÆ’Ã‚Â³ la factura",
                                "idfactura", idfactura
                        ));
            }

            String xmlAutorizado = factura.getXmlautorizado();
            if (xmlAutorizado == null || xmlAutorizado.isBlank()) {
                // Factura sÃƒÆ’Ã‚Â­ existe, pero aÃƒÆ’Ã‚Âºn no tiene XML autorizado
                return ResponseEntity.status(HttpStatus.CONFLICT)
                        .body(Map.of(
                                "codigo", "XML_AUTORIZADO_NO_ENCONTRADO",
                                "error", "La factura aÃƒÆ’Ã‚Âºn no cuenta con XML autorizado",
                                "idfactura", idfactura
                        ));
            }

            // 2) Determinar plantilla segÃƒÆ’Ã‚Âºn fecha
            LocalDate fechaEmision = LocalDate.from(factura.getFechaemision());
            LocalDate fechaLimite  = LocalDate.of(2025, 5, 6);

            ByteArrayOutputStream pdfStream;
            if (fechaEmision != null && fechaEmision.isBefore(fechaLimite)) {
                pdfStream = xmlToPdfService.generarFacturaPDF_v2(xmlAutorizado);
            } else {
                pdfStream = xmlToPdfService.generarFacturaPDF_v3(xmlAutorizado);
            }

            if (pdfStream == null || pdfStream.size() == 0) {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body(Map.of(
                                "codigo", "PDF_VACIO",
                                "error", "No se pudo generar el PDF (stream vacio).",
                                "idfactura", idfactura
                        ));
            }

            // 3) Preparar la descarga
            InputStreamResource resource =
                    new InputStreamResource(new ByteArrayInputStream(pdfStream.toByteArray()));

            String nombreArchivo = "factura_" + idfactura + ".pdf";

            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + nombreArchivo)
                    .contentType(MediaType.APPLICATION_PDF)
                    .contentLength(pdfStream.size())
                    .body(resource);

        } catch (org.springframework.web.client.RestClientException ex) {
            return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
                    .body(Map.of(
                            "codigo", "ERROR_COMUNICACION_MS",
                            "error", "No se pudo consultar el microservicio de facturas",
                            "detalle", ex.getMessage(),
                            "idfactura", idfactura
                    ));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of(
                            "codigo", "ERROR_GENERANDO_PDF",
                            "error", "Error generando el PDF",
                            "detalle", e.getMessage(),
                            "idfactura", idfactura
                    ));
        }
    }

    // ===================== 8) CREAR XML FIRMAR Y ENVIAR =====================
    @GetMapping(path = "/factura_electronica")
    public ResponseEntity<?> crear_firmar_enviarFactura(@RequestParam Long idfactura) {
        //crear variable de respuesta estado y detalla
        // 1) Intentar obtener factura
        Factura factura = fecFacturaR.findByIdfactura(idfactura);
        try {
            String modo = "XADES_BES";
            Integer ambienteForzado = 1;


            if(factura != null && (Objects.equals(factura.getEstado(), "A") || Objects.equals(factura.getEstado(), "O"))){
                return ResponseEntity.status(201).body(Map.of(
                        "error", "No se pudo generar la factura porque ya esta fue enviada",
                        "idfactura", idfactura
                ));
            }
            if (factura == null) {
                Boolean request = facturasService.findByIdfactura(idfactura);
                if (Boolean.TRUE.equals(request)) {
                    String url = backendBaseUrl + "/fec_factura/createFacElectro?idfactura=" + idfactura;
                    restTemplate.getForObject(url, Void.class);

                    // ÃƒÂ°Ã…Â¸Ã¢â‚¬ÂÃ‚Â¹ Reintentos: esperar hasta que ya exista en BD
                    for (int i = 0; i < 5; i++) {
                        Thread.sleep(2000); // espera 2s
                        factura = fecFacturaR.findByIdfactura(idfactura); // ÃƒÂ°Ã…Â¸Ã¢â‚¬ÂÃ‚Â¹ RECONSULTA aquÃƒÆ’Ã‚Â­
                        if (factura != null) break;
                    }

                    if (factura == null) {
                        return ResponseEntity.status(404).body(Map.of(
                                "error", "No se pudo generar la factura en los reintentos",
                                "idfactura", idfactura
                        ));
                    }

                } else {
                    return ResponseEntity.badRequest().body(Map.of(
                            "error", "Factura no pagada o no encontrada en facturasService",
                            "idfactura", idfactura
                    ));
                }
            }
            String estadoRaw = factura.getEstado();
            String estado = normEstado(estadoRaw);

            if (!"I".equalsIgnoreCase(estado)) {
                return ResponseEntity.status(404).body(Map.of(
                        "error", "No se pudo enviar la factura: el estado debe ser 'I'",
                        "estadoActual", estado,
                        "idfactura", idfactura
                ));
            }

            // 2) Generar XML sin firmar
            String xmlPlano = facturaXmlGeneratorService.generarXmlFactura(factura);

            // Limpia BOM si existe (importante para parseo/firma)
            if (xmlPlano != null && !xmlPlano.isEmpty() && xmlPlano.charAt(0) == '\uFEFF') {
                xmlPlano = xmlPlano.substring(1);
            }

            // 3) Elegir modo de firma
            ModoFirma mf = "XMLDSIG".equalsIgnoreCase(modo) ? ModoFirma.XMLDSIG : ModoFirma.XADES_BES;

            // 4) Firmar
            String xmlFirmado = firmaService.firmarFactura(xmlPlano, mf);

            // 5) Ambiente
            if (ambienteForzado != null) {
                sendXmlToSriService.setAmbiente(ambienteForzado == 2 ? 2 : 1);
            } else {
                sendXmlToSriService.setAmbienteFromXml(xmlFirmado);
            }

            // 6) Enviar a recepciÃƒÆ’Ã‚Â³n
            RespuestaSolicitud recepcion = sendXmlToSriService.enviarFacturaFirmadaTxt(xmlFirmado);

            if (!"RECIBIDA".equalsIgnoreCase(recepcion.getEstado())) {
                return ResponseEntity.badRequest().body(Map.of(
                        "estado", recepcion.getEstado(),
                        "errores", resumenErroresRecepcion(recepcion)
                ));
            }

            // 7) Polling/consulta de autorizacion
            RespuestaComprobante rc = sendXmlToSriService.consultarAutorizacionConEspera(
                    xmlFirmado,
                    clave -> {
                        try { return sendXmlToSriService.consultarAutorizacion(clave); }
                        catch (Exception e) { throw new RuntimeException(e); }
                    },
                    20,   // intentos
                    4000  // ms entre intentos
            );

            // 8) Extraer el XML autorizado (si existe)
            String xmlAutorizado = extraerXmlAutorizado(rc);
            if (xmlAutorizado != null) {
                // Generar PDF desde el XML autorizado
                ByteArrayOutputStream pdfStream = xmlToPdfService.generarFacturaPDF_v2(xmlAutorizado);
                if (pdfStream == null || pdfStream.size() == 0) {
                    return ResponseEntity.status(500).body(Map.of(
                            "error", "No se pudo generar el PDF de la autorizacion"
                    ));
                }
                // Persistir en BD (opcional)
                factura.setXmlautorizado(xmlAutorizado);
                // --- Preparar adjuntos (Base64) ---
                String nombrePdf  = "factura_" + factura.getIdfactura() + ".pdf";
                String nombreXml  = "factura_" + factura.getIdfactura() + ".xml";

                byte[] pdfBytes = pdfStream.toByteArray();
                byte[] xmlBytes = xmlAutorizado.getBytes(java.nio.charset.StandardCharsets.UTF_8);

                String pdfBase64 = java.util.Base64.getEncoder().encodeToString(pdfBytes);
                String xmlBase64 = java.util.Base64.getEncoder().encodeToString(xmlBytes);

                List<AttachmentDTO> attachments = java.util.List.of(
                        new AttachmentDTO(nombrePdf, "application/pdf", pdfBase64),
                        new AttachmentDTO(nombreXml, "application/xml", xmlBase64)
                );
                // --- Destinatarios ---
                // Usa el correo del comprador si lo tienes en la entidad; de lo contrario, uno de prueba
                /*List<String> to = (factura.getEmailcomprador() != null && !factura.getEmailcomprador().isBlank())
                        ? java.util.List.of(factura.getEmailcomprador().trim())
                        : java.util.List.of("destinatario@dominio.com");*/

                List<String> to = (java.util.List.of("ortizln9@gmail.com", "alexis.ortiz81@outlook.com"));

// CC/BCC opcionales
                List<String> cc = java.util.Collections.emptyList();
                List<String> bcc = java.util.Collections.emptyList();

// From: pÃƒÆ’Ã‚Â¡salo como null para que el servicio use app.mail.from
                String from = null;

// Asunto y cuerpo
                String subject = "Factura electrÃƒÆ’Ã‚Â³nica #" + factura.getEstablecimiento() + "-"
                        + factura.getPuntoemision() + "-" + factura.getSecuencial();

                String htmlBody =
                        "<h1>Factura electrÃƒÆ’Ã‚Â³nica autorizada</h1>" +
                                "<p>Estimado/a " + (factura.getRazonsocialcomprador() != null ? factura.getRazonsocialcomprador() : "cliente") + ",</p>" +
                                "<p>Adjuntamos su comprobante electrÃƒÆ’Ã‚Â³nico en formato PDF y XML.</p>" +
                                "<p>Saludos,<br>EPMAPA-T</p>";

// Inline images (si no usas, envÃƒÆ’Ã‚Â­a map vacio)
                java.util.Map<String, String> inlineImages = java.util.Collections.emptyMap();

                // --- Construir request ---
                SendMailRequest mailReq = new SendMailRequest(
                        from,      // deja que MailService tome el 'from' por defecto de configuraciÃƒÆ’Ã‚Â³n
                        to,
                        cc,
                        bcc,
                        subject,
                        htmlBody,
                        attachments,
                        inlineImages
                );
                // --- Enviar ---
                try {
                    java.util.UUID emailQueueId = mailService.send(mailReq, factura.getClaveacceso());
                    factura.setEstado("A");
                    factura.setErrores(null);
                    fecFacturaR.save(factura);
                    return ResponseEntity.ok(java.util.Map.of(
                            "ok", true,
                            "mensaje", "Factura encolada correctamente para envio por correo",
                            "idfactura", factura.getIdfactura(),
                            "claveAcceso", factura.getClaveacceso(),
                            "emailQueueId", emailQueueId
                    ));
                } catch (Exception mailEx) {
                    factura.setEstado("O");
                    factura.setErrores(mailEx.getMessage());
                    fecFacturaR.save(factura);
                    return ResponseEntity.status(503).body(java.util.Map.of(
                            "ok", false,
                            "estado", "CORREO_NO_DISPONIBLE",
                            "mensaje", "La factura ya esta autorizada, pero el servicio de correo no respondio correctamente.",
                            "detalle", mailEx.getMessage(),
                            "idfactura", factura.getIdfactura(),
                            "claveAcceso", factura.getClaveacceso()
                    ));
                }
            }
            factura.setEstado("P");
            factura.setErrores("Pendiente la autorizacion: " + xmlFirmado);
            fecFacturaR.save(factura);
            // 9) Si no hay autorizacion aÃƒÆ’Ã‚Âºn
            return ResponseEntity.status(202).body(Map.of(
                    "estado", "SIN_AUTORIZACION_EN_SRI",
                    "detalle", "La autorizacion aÃƒÆ’Ã‚Âºn no estÃƒÆ’Ã‚Â¡ disponible."
            ));

        } catch (Exception e) {
            e.printStackTrace();
            factura.setEstado("U");
            factura.setErrores(e.getMessage());
            fecFacturaR.save(factura);
            return ResponseEntity.status(500).body(Map.of(
                    "error", "Error al firmar/enviar comprobante",
                    "detalle", e.getMessage()
            ));
        }
    }



    /*
    * ========================================================================================================
                                            * RETENCIONES
    * ========================================================================================================
    */

    // ===================== 1) FIRMAR Y ENVIAR RETENCIÃƒÆ’Ã¢â‚¬Å“N (solo retorna XML autorizado) =====================
    @PostMapping(
            path = "/retencion",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE,
            produces = { MediaType.APPLICATION_XML_VALUE, MediaType.APPLICATION_JSON_VALUE }
    )
    public ResponseEntity<?> firmarYEnviarRetencion(
            @RequestPart("xml") MultipartFile xmlFile,
            @RequestParam(value = "modo", required = false, defaultValue = "XADES_BES") String modo,
            @RequestParam(value = "ambiente", required = false) Integer ambienteForzado,
            @RequestParam(value = "download", required = false, defaultValue = "false") boolean download, // si quieres forzar descarga
            @RequestParam(value = "diagnostics", required = false, defaultValue = "false") boolean diagnostics
    ) {
        long started = System.currentTimeMillis();
        String requestId = UUID.randomUUID().toString();
        try {
            MDC.put("requestId", requestId);
            MDC.put("tipoDocumento", "RETENCION");
            MDC.put("etapaActual", "RECEPCION_XML");
            // 1) XML en String (limpiando BOM por si acaso)
            String xmlPlano = toUtf8String(xmlFile);
            var validation = retencionValidationService.validate(xmlPlano);
            putIfPresent("claveAcceso", validation.claveAcceso());
            if (!validation.valid()) {
                log.warn("Retencion rechazada por validacion previa: {}", validation.errors());
                return ResponseEntity.badRequest().body(Map.of(
                        "estado", "VALIDACION_PREVIA_FALLIDA",
                        "requestId", requestId,
                        "errores", validation.errors(),
                        "warnings", validation.warnings()
                ));
            }

            // 2) Modo firma
            ModoFirma mf = "XMLDSIG".equalsIgnoreCase(modo) ? ModoFirma.XMLDSIG : ModoFirma.XADES_BES;

            // 3) Firmar usando el definirId indicado
            MDC.put("etapaActual", "FIRMA");
            String xmlFirmado = firmaService.firmarFactura(xmlPlano, mf);

            int ambienteSolicitud = ambienteForzado != null
                    ? (ambienteForzado == 2 ? 2 : 1)
                    : sendXmlToSriService.inferAmbienteFromXml(xmlFirmado);
            MDC.put("ambiente", String.valueOf(ambienteSolicitud));

            // 5) Enviar a recepciÃƒÆ’Ã‚Â³n
            MDC.put("etapaActual", "RECEPCION_SRI");
            RespuestaSolicitud recepcion = sendXmlToSriService.enviarFacturaFirmadaTxt(xmlFirmado, ambienteSolicitud);

            // 6) Si recepciÃƒÆ’Ã‚Â³n NO fue RECIBIDA -> 400 con errores
            if (!"RECIBIDA".equalsIgnoreCase(recepcion.getEstado())) {
                log.warn("Retencion devuelta por recepcion SRI estado={} errores={}", recepcion.getEstado(), resumenErroresRecepcion(recepcion));
                return ResponseEntity.badRequest().body(respuestaRecepcionDevuelta(requestId, validation, recepcion, xmlFirmado, diagnostics, started));
            }
            // 7) Polling de autorizacion
            MDC.put("etapaActual", "AUTORIZACION_SRI");
            RespuestaComprobante rc = sendXmlToSriService.consultarAutorizacionConEspera(
                    xmlFirmado,
                    ambienteSolicitud,
                    10, 4000
            );
            // 8) Extraer solo el XML autorizado del SRI
            String xmlAutorizado = extraerXmlAutorizado(rc);
            if (xmlAutorizado != null) {
                if (download) {
                    // respuesta como adjunto para que el navegador descargue
                    String fileName = "retencion-" + System.currentTimeMillis() + ".xml";
                    return ResponseEntity.ok()
                            .contentType(MediaType.APPLICATION_XML)
                            .header("Content-Disposition", "attachment; filename=\"" + fileName + "\"")
                            .body(xmlAutorizado);
                }
                // o solo el XML (texto) con application/xml
                return ResponseEntity.ok()
                        .contentType(MediaType.APPLICATION_XML)
                        .body(xmlAutorizado);
            }
            // 9) AÃƒÆ’Ã‚Âºn no autorizado
            return ResponseEntity.status(202).body(respuestaSinAutorizacion(requestId, validation, rc, xmlFirmado, diagnostics, started));
        } catch (Exception e) {
            log.error("Error al procesar retencion", e);
            return ResponseEntity.status(500).body(Map.of(
                    "error", "Error al firmar/enviar comprobante de retenciÃƒÆ’Ã‚Â³n",
                    "detalle", e.getMessage(),
                    "requestId", requestId,
                    "tiempoProcesoMs", System.currentTimeMillis() - started
            ));
        } finally {
            MDC.clear();
        }
    }



    // ===================== 5) FIRMAR Y ENVIAR RETENCIÃƒÆ’Ã¢â‚¬Å“N (recibe XML como String) =====================
    @PostMapping(
            path = "/retencion/string",
            consumes = { MediaType.APPLICATION_XML_VALUE, MediaType.TEXT_XML_VALUE },
            produces = { MediaType.APPLICATION_XML_VALUE, MediaType.APPLICATION_JSON_VALUE }
    )
    public ResponseEntity<?> firmarYEnviarRetencion(
            @RequestBody String xmlPlanoBody,
            @RequestParam(value = "modo", required = false, defaultValue = "XADES_BES") String modo,
            @RequestParam(value = "ambiente", required = false) Integer ambienteForzado,
            @RequestParam(value = "download", required = false, defaultValue = "false") boolean download,
            @RequestParam(value = "diagnostics", required = false, defaultValue = "false") boolean diagnostics
    ) {
        long started = System.currentTimeMillis();
        String requestId = UUID.randomUUID().toString();
        try {
            MDC.put("requestId", requestId);
            MDC.put("tipoDocumento", "RETENCION");
            MDC.put("etapaActual", "RECEPCION_XML");
            // 1) Validar y limpiar el XML recibido
            if (xmlPlanoBody == null || xmlPlanoBody.isBlank()) {
                return ResponseEntity.badRequest().body(Map.of(
                        "error", "XML vacio o nulo"
                ));
            }
            String xmlPlano = stripBom(xmlPlanoBody).trim();
            var validation = retencionValidationService.validate(xmlPlano);
            putIfPresent("claveAcceso", validation.claveAcceso());
            if (!validation.valid()) {
                log.warn("Retencion rechazada por validacion previa: {}", validation.errors());
                return ResponseEntity.badRequest().body(Map.of(
                        "estado", "VALIDACION_PREVIA_FALLIDA",
                        "requestId", requestId,
                        "errores", validation.errors(),
                        "warnings", validation.warnings()
                ));
            }

            // 2) Modo firma
            ModoFirma mf = "XMLDSIG".equalsIgnoreCase(modo) ? ModoFirma.XMLDSIG : ModoFirma.XADES_BES;

            // 3) Firmar (re-usa tu servicio actual)
            MDC.put("etapaActual", "FIRMA");
            String xmlFirmado = firmaService.firmarFactura(xmlPlano, mf);

            int ambienteSolicitud = ambienteForzado != null
                    ? (ambienteForzado == 2 ? 2 : 1)
                    : sendXmlToSriService.inferAmbienteFromXml(xmlFirmado);
            MDC.put("ambiente", String.valueOf(ambienteSolicitud));

            // 5) Enviar a recepciÃƒÆ’Ã‚Â³n
            MDC.put("etapaActual", "RECEPCION_SRI");
            RespuestaSolicitud recepcion = sendXmlToSriService.enviarFacturaFirmadaTxt(xmlFirmado, ambienteSolicitud);

            // 6) Si recepciÃƒÆ’Ã‚Â³n NO fue RECIBIDA -> 400 con errores
            if (!"RECIBIDA".equalsIgnoreCase(recepcion.getEstado())) {
                log.warn("Retencion devuelta por recepcion SRI estado={} errores={}", recepcion.getEstado(), resumenErroresRecepcion(recepcion));
                return ResponseEntity.badRequest().body(respuestaRecepcionDevuelta(requestId, validation, recepcion, xmlFirmado, diagnostics, started));
            }

            // 7) Polling de autorizacion
            MDC.put("etapaActual", "AUTORIZACION_SRI");
            RespuestaComprobante rc = sendXmlToSriService.consultarAutorizacionConEspera(
                    xmlFirmado,
                    ambienteSolicitud,
                    30,   // intentos
                    4000  // ms entre intentos
            );

            // 8) Extraer XML autorizado
            String xmlAutorizado = extraerXmlAutorizado(rc);
            if (xmlAutorizado != null && !xmlAutorizado.isBlank()) {
                if (download) {
                    String fileName = "retencion-" + System.currentTimeMillis() + ".xml";
                    return ResponseEntity.ok()
                            .contentType(MediaType.APPLICATION_XML)
                            .header("Content-Disposition", "attachment; filename=\"" + fileName + "\"")
                            .body(xmlAutorizado);
                }
                return ResponseEntity.ok()
                        .contentType(MediaType.APPLICATION_XML)
                        .body(xmlAutorizado);
            }
            return ResponseEntity.status(202).body(respuestaSinAutorizacion(requestId, validation, rc, xmlFirmado, diagnostics, started));

        } catch (Exception e) {
            log.error("Error al procesar retencion", e);
            return ResponseEntity.status(500).body(Map.of(
                    "error", "Error al firmar/enviar comprobante de retenciÃƒÆ’Ã‚Â³n",
                    "detalle", e.getMessage(),
                    "requestId", requestId,
                    "tiempoProcesoMs", System.currentTimeMillis() - started
            ));
        } finally {
            MDC.clear();
        }
    }


    @PostMapping(value = "/retenciones/pdf", consumes = MediaType.APPLICATION_XML_VALUE, produces = MediaType.APPLICATION_PDF_VALUE)
    public ResponseEntity<byte[]> pdf(@RequestBody String xmlAutorizacion) throws Exception {
        byte[] pdf = retencionPdfService.generarPdfDesdeXmlAutorizado(xmlAutorizacion);
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=retencion.pdf")
                .body(pdf);
    }

    private static void putIfPresent(String key, String value) {
        if (value != null && !value.isBlank()) {
            MDC.put(key, value);
        }
    }

    private static Map<String, Object> respuestaRecepcionDevuelta(
            String requestId,
            SriRetencionValidationService.RetencionValidationResult validation,
            RespuestaSolicitud recepcion,
            String xmlFirmado,
            boolean diagnostics,
            long started
    ) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("estado", recepcion == null ? "SIN_RESPUESTA_RECEPCION" : recepcion.getEstado());
        body.put("requestId", requestId);
        body.put("etapaActual", "RECEPCION_SRI");
        body.put("tipoDocumento", "RETENCION");
        body.put("claveAcceso", validation.claveAcceso());
        body.put("ambiente", validation.ambiente());
        body.put("codDoc", validation.codDoc());
        body.put("version", validation.version());
        body.put("errores", resumenErroresRecepcion(recepcion));
        body.put("warningsValidacion", validation.warnings());
        body.put("tiempoProcesoMs", System.currentTimeMillis() - started);
        if (diagnostics) {
            body.put("xmlFirmado", xmlFirmado);
        }
        return body;
    }

    private static Map<String, Object> respuestaSinAutorizacion(
            String requestId,
            SriRetencionValidationService.RetencionValidationResult validation,
            RespuestaComprobante rc,
            String xmlFirmado,
            boolean diagnostics,
            long started
    ) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("estado", "SIN_AUTORIZACION_EN_SRI");
        body.put("detalle", "La autorizacion aun no esta disponible o el SRI devolvio una autorizacion no valida.");
        body.put("requestId", requestId);
        body.put("etapaActual", "AUTORIZACION_SRI");
        body.put("tipoDocumento", "RETENCION");
        body.put("claveAcceso", validation.claveAcceso());
        body.put("ambiente", validation.ambiente());
        body.put("codDoc", validation.codDoc());
        body.put("version", validation.version());
        body.put("tiempoProcesoMs", System.currentTimeMillis() - started);
        if (rc != null) {
            body.put("respuestaAutorizacion", mapRespuestaAutorizacionBasica(rc));
        }
        if (diagnostics) {
            body.put("xmlFirmado", xmlFirmado);
        }
        return body;
    }

    private static Map<String, Object> mapRespuestaAutorizacionBasica(RespuestaComprobante rc) {
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("numeroComprobantes", rc.getNumeroComprobantes());
        List<Map<String, Object>> autorizaciones = new ArrayList<>();
        if (rc.getAutorizaciones() != null && rc.getAutorizaciones().getAutorizacion() != null) {
            for (var aut : rc.getAutorizaciones().getAutorizacion()) {
                Map<String, Object> item = new LinkedHashMap<>();
                item.put("estado", safe(aut.getEstado()));
                item.put("numeroAutorizacion", safe(aut.getNumeroAutorizacion()));
                item.put("fechaAutorizacion", aut.getFechaAutorizacion() == null ? null : aut.getFechaAutorizacion().toString());
                if (aut.getMensajes() != null && aut.getMensajes().getMensaje() != null) {
                    List<Map<String, String>> mensajes = new ArrayList<>();
                    for (var m : aut.getMensajes().getMensaje()) {
                        mensajes.add(Map.of(
                                "identificador", safe(m.getIdentificador()),
                                "mensaje", safe(m.getMensaje()),
                                "informacionAdicional", safe(m.getInformacionAdicional())
                        ));
                    }
                    item.put("mensajes", mensajes);
                }
                autorizaciones.add(item);
            }
        }
        out.put("autorizaciones", autorizaciones);
        return out;
    }

    // ========== 1) Consultar por CLAVE DE ACCESO ==========
    @GetMapping(
            value = "/autorizacion",
            produces = { MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE }
    )
    public ResponseEntity<?> consultarAutorizacion(
            @RequestParam String claveAcceso,
            @RequestParam(defaultValue = "false") boolean wait,        // true = polling
            @RequestParam(defaultValue = "60") int attempts,           // intentos de polling
            @RequestParam(defaultValue = "8000") long sleepMillis,     // pausa base entre intentos (ms)
            @RequestParam(defaultValue = "true") boolean returnXml,    // true = retornar application/xml
            @RequestParam(defaultValue = "false") boolean download     // true = forzar descarga
    ) {
        try {
            RespuestaComprobante rc;
            // =========================
            // 1) Consulta con o sin polling
            // =========================
            if (wait) {
                rc = sendXmlToSriService.consultarAutorizacionConEsperaPorClave(
                        claveAcceso.trim(),
                        attempts,
                        sleepMillis,
                        sleepMillis * 4,
                        1.5,
                        true
                );
            } else {
                rc = sendXmlToSriService.consultarAutorizacion(claveAcceso.trim());
            }

            if (rc == null) {
                return ResponseEntity.status(500).body(Map.of(
                        "error", "Respuesta nula del servicio de autorizacion del SRI",
                        "claveAcceso", claveAcceso
                ));
            }

            // =========================
            // 2) Normalizar nÃƒÆ’Ã‚Âºmero de comprobantes
            // =========================
            int num = 0;
            try {
                num = (rc.getNumeroComprobantes() == null)
                        ? 0
                        : Integer.parseInt(rc.getNumeroComprobantes().trim());
            } catch (NumberFormatException ignore) { }

            if (num <= 0
                    || rc.getAutorizaciones() == null
                    || rc.getAutorizaciones().getAutorizacion() == null
                    || rc.getAutorizaciones().getAutorizacion().isEmpty()) {

                return ResponseEntity.status(202).body(Map.of(
                        "estado", "SIN_AUTORIZACION_EN_SRI",
                        "detalle", "AÃƒÆ’Ã‚Âºn no hay autorizaciones disponibles para la clave.",
                        "claveAcceso", claveAcceso
                ));
            }

            var lista = rc.getAutorizaciones().getAutorizacion();

            // =========================
            // 3) Intentar encontrar una AUTORIZADO
            // =========================
            var autorizada = primeraAutorizacionAutorizada(lista);

            if (autorizada == null) {
                var a0 = lista.get(0);
                var mensajes = (a0.getMensajes() != null && a0.getMensajes().getMensaje() != null)
                        ? a0.getMensajes().getMensaje().stream().map(m -> Map.of(
                        "identificador", safeStr(m.getIdentificador()),
                        "mensaje", safeStr(m.getMensaje()),
                        "informacionAdicional", safeStr(m.getInformacionAdicional())
                )).toList()
                        : java.util.List.of();

                return ResponseEntity.status(400).body(Map.of(
                        "estadoAutorizacion", safeStr(a0.getEstado()),
                        "numeroAutorizacion", safeStr(a0.getNumeroAutorizacion()),
                        "fechaAutorizacion", (a0.getFechaAutorizacion() != null
                                ? a0.getFechaAutorizacion().toString()
                                : ""),
                        "ambiente", safeStr(a0.getAmbiente()),
                        "mensajes", mensajes,
                        "claveAcceso", claveAcceso
                ));
            }

            // =========================
            // 4) Construir XML COMPLETO DE AUTORIZACION
            // =========================

            // El comprobante (factura) viene como String en <comprobante><![CDATA[...]]>
            String xmlFactura = cleanComprobanteXml(autorizada.getComprobante());
            if (xmlFactura.isBlank()) {
                return ResponseEntity.status(500).body(Map.of(
                        "error", "La autorizacion no contiene XML de comprobante.",
                        "claveAcceso", claveAcceso
                ));
            }

            String estado = normEstado(autorizada.getEstado());
            String numeroAutorizacion = safeStr(autorizada.getNumeroAutorizacion());
            String fechaAutorizacion = (autorizada.getFechaAutorizacion() != null
                    ? autorizada.getFechaAutorizacion().toXMLFormat()
                    : "");
            String ambiente = safeStr(autorizada.getAmbiente());

            // XML completo como lo devuelve el SRI (wrapper <autorizacion> + comprobante en CDATA)
            String xmlAutorizacionCompleta =
                    "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                            "<autorizacion>\n" +
                            "  <estado>" + estado + "</estado>\n" +
                            "  <numeroAutorizacion>" + numeroAutorizacion + "</numeroAutorizacion>\n" +
                            "  <fechaAutorizacion>" + fechaAutorizacion + "</fechaAutorizacion>\n" +
                            "  <ambiente>" + ambiente + "</ambiente>\n" +
                            "  <comprobante><![CDATA[" + xmlFactura + "]]></comprobante>\n" +
                            "</autorizacion>";

            // =========================
            // 5) Modos de respuesta
            // =========================

            // 1) Forzar descarga del XML **COMPLETO**
            if (download) {
                String nombre = "autorizacion_" + claveAcceso + ".xml";
                return ResponseEntity.ok()
                        .contentType(MediaType.APPLICATION_XML)
                        .header("Content-Disposition", "attachment; filename=\"" + nombre + "\"")
                        .body(xmlAutorizacionCompleta);
            }

            // 2) Retornar directamente el XML como application/xml (wrapper + fecha + nÃƒÆ’Ã‚Âºmero + factura)
            if (returnXml) {
                return ResponseEntity.ok()
                        .contentType(MediaType.APPLICATION_XML)
                        .body(xmlAutorizacionCompleta);
            }

            // 3) Resumen JSON (sin XML), incluyendo nÃƒÆ’Ã‚Âºmero y fecha de autorizacion
            return ResponseEntity.ok(Map.of(
                    "estado", "AUTORIZADO",
                    "numeroAutorizacion", numeroAutorizacion,
                    "fechaAutorizacion", fechaAutorizacion,
                    "ambiente", ambiente,
                    "claveAcceso", claveAcceso
            ));

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body(Map.of(
                    "error", "Error consultando autorizacion en SRI",
                    "detalle", e.getMessage(),
                    "claveAcceso", claveAcceso
            ));
        }
    }

    @GetMapping("/retencion/download")
    public ResponseEntity<?> descargarRetencionAutorizada(
            @RequestParam String claveAcceso,
            @RequestParam(defaultValue = "zip") String downloadType, // zip | pdf | xml
            @RequestParam(defaultValue = "false") boolean wait,
            @RequestParam(defaultValue = "60") int attempts,
            @RequestParam(defaultValue = "8000") long sleepMillis
    ) {
        try {
            RespuestaComprobante rc;

            if (wait) {
                rc = sendXmlToSriService.consultarAutorizacionConEsperaPorClave(
                        claveAcceso.trim(),
                        attempts,
                        sleepMillis,
                        sleepMillis * 4,
                        1.5,
                        true
                );
            } else {
                rc = sendXmlToSriService.consultarAutorizacion(claveAcceso.trim());
            }


            if (rc == null) {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                        "error", "Respuesta nula del servicio de autorizacion del SRI",
                        "claveAcceso", claveAcceso
                ));
            }

            int num = 0;
            try {
                num = (rc.getNumeroComprobantes() == null) ? 0 : Integer.parseInt(rc.getNumeroComprobantes().trim());
            } catch (NumberFormatException ignore) {}

            if (num <= 0
                    || rc.getAutorizaciones() == null
                    || rc.getAutorizaciones().getAutorizacion() == null
                    || rc.getAutorizaciones().getAutorizacion().isEmpty()) {
                return ResponseEntity.status(HttpStatus.ACCEPTED).body(Map.of(
                        "estado", "SIN_AUTORIZACION_EN_SRI",
                        "detalle", "AÃƒÆ’Ã‚Âºn no hay autorizaciones disponibles para la clave.",
                        "claveAcceso", claveAcceso
                ));
            }

            var lista = rc.getAutorizaciones().getAutorizacion();

            var autorizada = primeraAutorizacionAutorizada(lista);

            if (autorizada == null) {
                var a0 = lista.get(0);
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of(
                        "estadoAutorizacion", safeStr(a0.getEstado()),
                        "numeroAutorizacion", safeStr(a0.getNumeroAutorizacion()),
                        "fechaAutorizacion", (a0.getFechaAutorizacion() != null ? a0.getFechaAutorizacion().toString() : ""),
                        "ambiente", safeStr(a0.getAmbiente()),
                        "claveAcceso", claveAcceso
                ));
            }

            String xmlComprobante = cleanComprobanteXml(autorizada.getComprobante());
            if (xmlComprobante.isBlank()) {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                        "error", "La autorizacion no contiene XML de comprobante.",
                        "claveAcceso", claveAcceso
                ));
            }

            String estado = normEstado(autorizada.getEstado());
            String numeroAutorizacion = safeStr(autorizada.getNumeroAutorizacion());
            String fechaAutorizacion = (autorizada.getFechaAutorizacion() != null
                    ? autorizada.getFechaAutorizacion().toXMLFormat()
                    : "");
            String ambiente = safeStr(autorizada.getAmbiente());

            String xmlAutorizacionCompleta =
                    "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                            "<autorizacion>\n" +
                            "  <estado>" + estado + "</estado>\n" +
                            "  <numeroAutorizacion>" + numeroAutorizacion + "</numeroAutorizacion>\n" +
                            "  <fechaAutorizacion>" + fechaAutorizacion + "</fechaAutorizacion>\n" +
                            "  <ambiente>" + ambiente + "</ambiente>\n" +
                            "  <comprobante><![CDATA[" + xmlComprobante + "]]></comprobante>\n" +
                            "</autorizacion>";

            String baseName = "retencion_" + claveAcceso;

            // ====== XML ======
            if ("xml".equalsIgnoreCase(downloadType)) {
                byte[] xmlBytes = xmlAutorizacionCompleta.getBytes(StandardCharsets.UTF_8);
                return ResponseEntity.ok()
                        .contentType(MediaType.APPLICATION_XML)
                        .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + baseName + ".xml\"")
                        .contentLength(xmlBytes.length)
                        .body(xmlBytes);
            }

            // ====== PDF ======
            byte[] pdfBytes = retencionPdfService.generarPdfDesdeXmlAutorizado(xmlAutorizacionCompleta);

            if ("pdf".equalsIgnoreCase(downloadType)) {
                return ResponseEntity.ok()
                        .contentType(MediaType.APPLICATION_PDF)
                        .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + baseName + ".pdf\"")
                        .contentLength(pdfBytes.length)
                        .body(pdfBytes);
            }

            // ====== ZIP (XML + PDF) ======
            byte[] zipBytes = buildZip(baseName, xmlAutorizacionCompleta, pdfBytes);

            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType("application/zip"))
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + baseName + ".zip\"")
                    .contentLength(zipBytes.length)
                    .body(zipBytes);

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                    "error", "Error generando descarga de retenciÃƒÆ’Ã‚Â³n",
                    "detalle", e.getMessage(),
                    "claveAcceso", claveAcceso
            ));
        }
    }


    @PostMapping("/retencion/mail")
    public ResponseEntity<?> enviarRetencionPorCorreo(
            @RequestParam String claveAcceso,
            @RequestParam String emailDestino,

            // polling opcional
            @RequestParam(defaultValue = "false") boolean wait,
            @RequestParam(defaultValue = "60") int attempts,
            @RequestParam(defaultValue = "8000") long sleepMillis
    ) {
        try {
            RespuestaComprobante rc;

            if (wait) {
                rc = sendXmlToSriService.consultarAutorizacionConEsperaPorClave(
                        claveAcceso.trim(),
                        attempts,
                        sleepMillis,
                        sleepMillis * 4,
                        1.5,
                        true
                );
            } else {
                rc = sendXmlToSriService.consultarAutorizacion(claveAcceso.trim());
            }

            if (rc == null || rc.getAutorizaciones() == null
                    || rc.getAutorizaciones().getAutorizacion() == null
                    || rc.getAutorizaciones().getAutorizacion().isEmpty()) {
                return ResponseEntity.status(202).body(java.util.Map.of(
                        "estado", "SIN_AUTORIZACION_EN_SRI",
                        "detalle", "AÃƒÆ’Ã‚Âºn no hay autorizaciones disponibles para la clave.",
                        "claveAcceso", claveAcceso
                ));
            }

            var lista = rc.getAutorizaciones().getAutorizacion();
            var autorizada = primeraAutorizacionAutorizada(lista);

            if (autorizada == null) {
                var a0 = lista.get(0);
                return ResponseEntity.status(400).body(java.util.Map.of(
                        "estadoAutorizacion", safeStr(a0.getEstado()),
                        "numeroAutorizacion", safeStr(a0.getNumeroAutorizacion()),
                        "fechaAutorizacion", (a0.getFechaAutorizacion() != null ? a0.getFechaAutorizacion().toString() : ""),
                        "ambiente", safeStr(a0.getAmbiente()),
                        "claveAcceso", claveAcceso
                ));
            }

            String xmlComprobante = cleanComprobanteXml(autorizada.getComprobante());
            if (xmlComprobante.isBlank()) {
                return ResponseEntity.status(500).body(java.util.Map.of(
                        "error", "La autorizacion no contiene XML de comprobante.",
                        "claveAcceso", claveAcceso
                ));
            }

            String estado = normEstado(autorizada.getEstado());
            String numeroAutorizacion = safeStr(autorizada.getNumeroAutorizacion());
            String fechaAutorizacion = (autorizada.getFechaAutorizacion() != null
                    ? autorizada.getFechaAutorizacion().toXMLFormat()
                    : "");
            String ambiente = safeStr(autorizada.getAmbiente());

            String xmlAutorizacionCompleta =
                    "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                            "<autorizacion>\n" +
                            "  <estado>" + estado + "</estado>\n" +
                            "  <numeroAutorizacion>" + numeroAutorizacion + "</numeroAutorizacion>\n" +
                            "  <fechaAutorizacion>" + fechaAutorizacion + "</fechaAutorizacion>\n" +
                            "  <ambiente>" + ambiente + "</ambiente>\n" +
                            "  <comprobante><![CDATA[" + xmlComprobante + "]]></comprobante>\n" +
                            "</autorizacion>";

            byte[] pdfBytes = retencionPdfService.generarPdfDesdeXmlAutorizado(xmlAutorizacionCompleta);

            String baseName = "retencion_" + claveAcceso.trim();
            String subject = "RetenciÃ³n electrÃ³nica - " + claveAcceso.trim();
            String body = "Se adjunta la retenciÃ³n electrÃ³nica en formato XML y PDF.\n\nClave de acceso: " + claveAcceso.trim();

            try {
                java.util.UUID emailQueueId = retencionEmailService.enviarRetencion(
                        emailDestino.trim(),
                        subject,
                        body,
                        baseName,
                        xmlAutorizacionCompleta,
                        pdfBytes
                );

                return ResponseEntity.ok(java.util.Map.of(
                        "ok", true,
                        "mensaje", "Correo encolado correctamente",
                        "email", emailDestino,
                        "claveAcceso", claveAcceso,
                        "emailQueueId", emailQueueId
                ));
            } catch (Exception mailEx) {
                return ResponseEntity.status(503).body(java.util.Map.of(
                        "ok", false,
                        "estado", "CORREO_NO_DISPONIBLE",
                        "mensaje", "La retenciÃ³n ya estÃ¡ autorizada, pero el servicio de correo no respondiÃ³ correctamente.",
                        "detalle", mailEx.getMessage(),
                        "email", emailDestino,
                        "claveAcceso", claveAcceso
                ));
            }
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body(java.util.Map.of(
                    "error", "Error enviando retenciÃ³n por correo",
                    "detalle", e.getMessage(),
                    "claveAcceso", claveAcceso
            ));
        }
    }
    @GetMapping("/retenciones/download")
    public ResponseEntity<?> descargarRetencionAutorizadaAlias(
            @RequestParam String claveAcceso,
            @RequestParam(defaultValue = "zip") String downloadType,
            @RequestParam(defaultValue = "false") boolean wait,
            @RequestParam(defaultValue = "60") int attempts,
            @RequestParam(defaultValue = "8000") long sleepMillis
    ) {
        return descargarRetencionAutorizada(claveAcceso, downloadType, wait, attempts, sleepMillis);
    }

    @GetMapping("/retenciones/pdf")
    public ResponseEntity<?> descargarRetencionPdfAlias(
            @RequestParam String claveAcceso,
            @RequestParam(defaultValue = "false") boolean wait,
            @RequestParam(defaultValue = "60") int attempts,
            @RequestParam(defaultValue = "8000") long sleepMillis
    ) {
        return descargarRetencionAutorizada(claveAcceso, "pdf", wait, attempts, sleepMillis);
    }

    @GetMapping("/retenciones/xml")
    public ResponseEntity<?> descargarRetencionXmlAlias(
            @RequestParam String claveAcceso,
            @RequestParam(defaultValue = "false") boolean wait,
            @RequestParam(defaultValue = "60") int attempts,
            @RequestParam(defaultValue = "8000") long sleepMillis
    ) {
        return descargarRetencionAutorizada(claveAcceso, "xml", wait, attempts, sleepMillis);
    }

    @PostMapping("/retenciones/mail")
    public ResponseEntity<?> enviarRetencionPorCorreoAlias(
            @RequestParam String claveAcceso,
            @RequestParam String emailDestino,
            @RequestParam(defaultValue = "false") boolean wait,
            @RequestParam(defaultValue = "60") int attempts,
            @RequestParam(defaultValue = "8000") long sleepMillis
    ) {
        return enviarRetencionPorCorreo(claveAcceso, emailDestino, wait, attempts, sleepMillis);
    }


    private static byte[] buildZip(String baseName, String xmlAutorizacionCompleta, byte[] pdfBytes) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (ZipOutputStream zos = new ZipOutputStream(baos)) {
            zos.putNextEntry(new ZipEntry(baseName + ".xml"));
            zos.write(xmlAutorizacionCompleta.getBytes(StandardCharsets.UTF_8));
            zos.closeEntry();

            zos.putNextEntry(new ZipEntry(baseName + ".pdf"));
            zos.write(pdfBytes);
            zos.closeEntry();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return baos.toByteArray();
    }

    // helper simple si no lo tienes:
    private String safeStr(Object o) {
        return (o == null) ? "" : o.toString().trim();
    }

    // ========== 2) Consultar por XML (extrae <claveAcceso>) ==========
    @PostMapping(
            path = "/autorizacion/by-xml",
            consumes = MediaType.APPLICATION_XML_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public ResponseEntity<?> consultarAutorizacionDesdeXml(
            @RequestBody String xml,
            @RequestParam(defaultValue = "false") boolean wait,
            @RequestParam(defaultValue = "10") int attempts,
            @RequestParam(defaultValue = "3000") long sleepMillis,
            @RequestParam(defaultValue = "false") boolean includeXml
    ) {
        try {
            if (xml == null || xml.isBlank()) {
                return ResponseEntity.badRequest().body(Map.of(
                        "error", "XML vacio"
                ));
            }
            String clave = extraerClaveAcceso(xml);
            if (clave == null || clave.isBlank()) {
                return ResponseEntity.badRequest().body(Map.of(
                        "error", "No se encontrÃƒÆ’Ã‚Â³ <claveAcceso> en el XML"
                ));
            }

            RespuestaComprobante rc;
            if (wait) {
                rc = sendXmlToSriService.consultarAutorizacionConEspera(
                        xml,
                        k -> {
                            try { return sendXmlToSriService.consultarAutorizacion(clave); }
                            catch (Exception e) { throw new RuntimeException(e); }
                        },
                        attempts,
                        sleepMillis
                );
            } else {
                rc = sendXmlToSriService.consultarAutorizacion(clave);
            }
            return ResponseEntity.ok( mapRespuestaAutorizacion(rc, includeXml) );
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body(Map.of(
                    "error", "Error consultando autorizacion en SRI",
                    "detalle", e.getMessage()
            ));
        }
    }
    // ================== Helpers ==================

    /** Estructura JSON amigable para el frontend */
    private Map<String, Object> mapRespuestaAutorizacion(RespuestaComprobante rc, boolean includeXml) {
        if (rc == null) {
            return Map.of("estado", "SIN_RESPUESTA");
        }

        String numero = (rc.getNumeroComprobantes() == null) ? "0" : rc.getNumeroComprobantes().trim();
        int n;
        try { n = Integer.parseInt(numero); } catch (NumberFormatException e) { n = 0; }

        List<Map<String, Object>> items = new ArrayList<>();
        if (rc.getAutorizaciones() != null && rc.getAutorizaciones().getAutorizacion() != null) {
            for (var a : rc.getAutorizaciones().getAutorizacion()) {
                Map<String, Object> it = new LinkedHashMap<>();
                it.put("estado", safe(a.getEstado()));
                it.put("numeroAutorizacion", safe(a.getNumeroAutorizacion()));
                it.put("fechaAutorizacion", a.getFechaAutorizacion() != null ? a.getFechaAutorizacion().toString() : null);
                it.put("ambiente", safe(a.getAmbiente()));

                // errores/mensajes (si existen)
                if (a.getMensajes() != null && a.getMensajes().getMensaje() != null && !a.getMensajes().getMensaje().isEmpty()) {
                    List<Map<String, String>> msgs = new ArrayList<>();
                    for (var m : a.getMensajes().getMensaje()) {
                        msgs.add(Map.of(
                                "identificador", safe(m.getIdentificador()),
                                "mensaje", safe(m.getMensaje()),
                                "informacionAdicional", safe(m.getInformacionAdicional())
                        ));
                    }
                    it.put("mensajes", msgs);
                }

                if (includeXml) {
                    String x = extraerComprobanteXmlAutorizado(a);
                    it.put("xmlAutorizado", x); // puede ser null si no hay
                }

                items.add(it);
            }
        }

        return Map.of(
                "numeroComprobantes", n,
                "autorizaciones", items
        );
    }

    private static String safe(String s) { return (s == null) ? "" : s; }

    /** Extrae <claveAcceso> de un XML texto */
    private String extraerClaveAcceso(String xml) {
        try {
            if (xml.charAt(0) == '\uFEFF') xml = xml.substring(1); // limpia BOM si vino
            javax.xml.parsers.DocumentBuilderFactory dbf = javax.xml.parsers.DocumentBuilderFactory.newInstance();
            dbf.setNamespaceAware(true);
            // endurece parser
            dbf.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            dbf.setFeature("http://xml.org/sax/features/external-general-entities", false);
            dbf.setFeature("http://xml.org/sax/features/external-parameter-entities", false);

            org.w3c.dom.Document doc = dbf.newDocumentBuilder()
                    .parse(new org.xml.sax.InputSource(new java.io.StringReader(xml)));
            org.w3c.dom.NodeList list = doc.getElementsByTagName("claveAcceso");
            if (list.getLength() == 0) return null;
            return list.item(0).getTextContent().replaceAll("\\s+", "");
        } catch (Exception ignore) {
            return null;
        }
    }

    /** Saca el <comprobante> XML dentro de una Autorizacion (si estÃƒÆ’Ã‚Â¡ presente) */
    private String extraerComprobanteXmlAutorizado(ec.gob.sri.ws.autorizacion.Autorizacion a) {
        try {
            if (a == null || a.getComprobante() == null) return null;
            // Suele venir como CDATA con el XML original del comprobante
            String xml = a.getComprobante();
            // normaliza BOM
            if (xml != null && !xml.isBlank() && xml.charAt(0) == '\uFEFF') xml = xml.substring(1);
            return xml;
        } catch (Exception e) {
            return null;
        }
    }

    /** Quita BOM UTF-8 si viene al inicio del String */
    private static String stripBom(String s) {
        if (s != null && !s.isEmpty() && s.charAt(0) == '\uFEFF') {
            return s.substring(1);
        }
        return s;
    }

    // Normaliza estado: null-safe, trim, sin acentos y sin espacios raros
    private static String normEstado(String s) {
        if (s == null) return "";
        String normalized = java.text.Normalizer.normalize(s, java.text.Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "");
        return normalized.trim().replaceAll("\\s+", " ").toUpperCase(java.util.Locale.ROOT);
    }

    private static String cleanComprobanteXml(String xml) {
        if (xml == null) return null;
        String comprobante = xml.trim();
        if (comprobante.startsWith("<![CDATA[")) {
            comprobante = comprobante.substring(9);
        }
        if (comprobante.endsWith("]]>")) {
            comprobante = comprobante.substring(0, comprobante.length() - 3);
        }
        if (!comprobante.isEmpty() && comprobante.charAt(0) == '\uFEFF') {
            comprobante = comprobante.substring(1);
        }
        return comprobante.trim();
    }

    private static boolean esAutorizacionValida(ec.gob.sri.ws.autorizacion.Autorizacion aut) {
        if (aut == null) {
            return false;
        }
        String estado = normEstado(aut.getEstado());
        String comprobante = cleanComprobanteXml(aut.getComprobante());
        return "AUTORIZADO".equals(estado) && comprobante != null && !comprobante.isBlank();
    }

    private static ec.gob.sri.ws.autorizacion.Autorizacion primeraAutorizacionAutorizada(
            java.util.List<ec.gob.sri.ws.autorizacion.Autorizacion> lista) {
        if (lista == null) {
            return null;
        }
        for (var aut : lista) {
            if (esAutorizacionValida(aut)) {
                return aut;
            }
        }
        return null;
    }

    /** Busca una <autorizacion> AUTORIZADA con <comprobante> no vacio y devuelve su XML. */
    private static String extraerXmlAutorizado(ec.gob.sri.ws.autorizacion.RespuestaComprobante rc) {
        if (rc == null || rc.getAutorizaciones() == null || rc.getAutorizaciones().getAutorizacion() == null) {
            return null;
        }

        for (var aut : rc.getAutorizaciones().getAutorizacion()) {
            if (esAutorizacionValida(aut)) {
                String comprobante = cleanComprobanteXml(aut.getComprobante());
                if (comprobante != null && !comprobante.isBlank()) {
                    return comprobante;
                }
            }
        }
        return null;
    }

    /** Convierte los detalles de errores de recepciÃƒÆ’Ã‚Â³n en una lista simple de mensajes. */
    private static List<String> resumenErroresRecepcion(ec.gob.sri.ws.recepcion.RespuestaSolicitud r) {
        List<String> out = new ArrayList<>();
        if (r != null && r.getComprobantes() != null && r.getComprobantes().getComprobante() != null) {
            r.getComprobantes().getComprobante().forEach(c -> {
                if (c.getMensajes() != null && c.getMensajes().getMensaje() != null) {
                    c.getMensajes().getMensaje().forEach(m -> {
                        StringBuilder sb = new StringBuilder();
                        if (m.getMensaje() != null) sb.append(m.getMensaje());
                        if (m.getInformacionAdicional() != null) sb.append(" - ").append(m.getInformacionAdicional());
                        out.add(sb.toString());
                    });
                }
            });
        }
        if (out.isEmpty() && r != null && r.getEstado() != null) {
            out.add("Estado: " + r.getEstado());
        }
        return out;
    }

    // ===== Manejador de errores comunes =====
    @ExceptionHandler({ IllegalArgumentException.class, IllegalStateException.class })
    public ResponseEntity<String> handleBadRequest(Exception ex) {
        return ResponseEntity.badRequest().body(ex.getMessage());
    }


    //======================================OPCIONES DE ENVIO DE CORREO ELECTRÃƒÆ’Ã¢â‚¬Å“NICO========================================================================

    @PostMapping("/send")
    public ResponseEntity<SendMailResponse> send(@Valid @RequestBody SendMailRequest req) {
        try {
            mailService.send(req);
            return ResponseEntity.ok(new SendMailResponse(true, "Enviado", java.time.Instant.now()));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(new SendMailResponse(false, e.getMessage(), java.time.Instant.now()));
        }
    }

    @PostMapping("/send-template")
    public ResponseEntity<SendMailResponse> sendTemplate(@Valid @RequestBody TemplateMailRequest req) {
        try {
            mailService.sendTemplate(req);
            return ResponseEntity.ok(new SendMailResponse(true, "Enviado", java.time.Instant.now()));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(new SendMailResponse(false, e.getMessage(), java.time.Instant.now()));
        }
    }

    @GetMapping("/health")
    public ResponseEntity<Map<String,Object>> health() {
        boolean ok = mailService.smtpHealth();
        return ResponseEntity.ok(Map.of("smtp", ok ? "UP" : "DOWN", "time", java.time.Instant.now()));
    }

}




