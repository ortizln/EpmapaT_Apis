package com.erp.sri_files.service;

import com.itextpdf.html2pdf.HtmlConverter;
import org.springframework.stereotype.Service;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import javax.xml.parsers.DocumentBuilderFactory;
import java.io.ByteArrayOutputStream;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;

@Service
public class BasicPdfDocumentService {

    public byte[] generarDesdeXml(String titulo, String xml) {
        String html = """
                <html>
                  <head>
                    <meta charset="UTF-8" />
                    <style>
                      body { font-family: Arial, sans-serif; padding: 24px; color: #1f2937; }
                      h1 { font-size: 20px; margin-bottom: 12px; }
                      p { margin-bottom: 16px; }
                      pre {
                        white-space: pre-wrap;
                        word-break: break-word;
                        background: #f3f4f6;
                        border: 1px solid #d1d5db;
                        border-radius: 8px;
                        padding: 16px;
                        font-size: 10px;
                      }
                    </style>
                  </head>
                  <body>
                    <h1>%s</h1>
                    <p>Representacion PDF basica del comprobante autorizado.</p>
                    <pre>%s</pre>
                  </body>
                </html>
                """.formatted(escape(titulo), escape(xml));

        ByteArrayOutputStream output = new ByteArrayOutputStream();
        HtmlConverter.convertToPdf(html, output);
        return output.toByteArray();
    }

    public byte[] generarGuiaRemisionDesdeXml(String xml) {
        try {
            Document document = parse(xml);
            String html = """
                    <html>
                      <head>
                        <meta charset="UTF-8" />
                        <style>
                          body { font-family: Arial, sans-serif; padding: 28px; color: #1f2937; font-size: 11px; }
                          h1 { font-size: 20px; margin: 0 0 8px 0; }
                          h2 { font-size: 14px; margin: 18px 0 8px 0; color: #0f766e; }
                          .subtitle { margin: 0 0 18px 0; color: #4b5563; }
                          .grid { width: 100%%; border-collapse: collapse; margin-bottom: 12px; }
                          .grid td, .grid th { border: 1px solid #d1d5db; padding: 6px 8px; vertical-align: top; }
                          .grid th { background: #ecfeff; text-align: left; }
                          .card { border: 1px solid #cbd5e1; border-radius: 8px; padding: 12px; margin-bottom: 14px; }
                          .muted { color: #6b7280; }
                        </style>
                      </head>
                      <body>
                        <h1>Guia de remision autorizada</h1>
                        <p class="subtitle">Representacion PDF basica del comprobante autorizado.</p>
                        <h2>Informacion tributaria</h2>
                        <table class="grid">
                          <tr><th>Clave de acceso</th><td>%s</td><th>RUC</th><td>%s</td></tr>
                          <tr><th>Emisor</th><td>%s</td><th>Documento</th><td>%s-%s-%s</td></tr>
                          <tr><th>Ambiente</th><td>%s</td><th>Direccion matriz</th><td>%s</td></tr>
                        </table>
                        <h2>Transporte</h2>
                        <table class="grid">
                          <tr><th>Partida</th><td>%s</td><th>Placa</th><td>%s</td></tr>
                          <tr><th>Transportista</th><td>%s</td><th>Identificacion</th><td>%s</td></tr>
                          <tr><th>Inicio</th><td>%s</td><th>Fin</th><td>%s</td></tr>
                        </table>
                        <h2>Destinatarios</h2>
                        %s
                      </body>
                    </html>
                    """.formatted(
                    escape(text(document, "claveAcceso")),
                    escape(text(document, "ruc")),
                    escape(text(document, "razonSocial")),
                    escape(text(document, "estab")),
                    escape(text(document, "ptoEmi")),
                    escape(text(document, "secuencial")),
                    escape(text(document, "ambiente")),
                    escape(text(document, "dirMatriz")),
                    escape(text(document, "dirPartida")),
                    escape(text(document, "placa")),
                    escape(text(document, "razonSocialTransportista")),
                    escape(text(document, "rucTransportista")),
                    escape(text(document, "fechaIniTransporte")),
                    escape(text(document, "fechaFinTransporte")),
                    buildDestinatariosHtml(document)
            );

            ByteArrayOutputStream output = new ByteArrayOutputStream();
            HtmlConverter.convertToPdf(html, output);
            return output.toByteArray();
        } catch (Exception ex) {
            return generarDesdeXml("Guia de remision autorizada", xml);
        }
    }

    public byte[] generarNotaCreditoDesdeXml(String xml) {
        return generarNotaComercialDesdeXml(
                xml,
                "Nota de credito autorizada",
                "notaCredito",
                "infoNotaCredito",
                "valorModificacion",
                "motivo"
        );
    }

    public byte[] generarNotaDebitoDesdeXml(String xml) {
        return generarNotaComercialDesdeXml(
                xml,
                "Nota de debito autorizada",
                "notaDebito",
                "infoNotaDebito",
                "valorTotal",
                "razon"
        );
    }

    private String escape(String value) {
        if (value == null) {
            return "";
        }
        return value
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;");
    }

    private Document parse(String xml) throws Exception {
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        dbf.setNamespaceAware(true);
        dbf.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
        dbf.setFeature("http://xml.org/sax/features/external-general-entities", false);
        dbf.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
        dbf.setXIncludeAware(false);
        dbf.setExpandEntityReferences(false);
        return dbf.newDocumentBuilder().parse(new InputSource(new StringReader(xml)));
    }

    private String text(Document document, String tagName) {
        NodeList nodes = document.getElementsByTagName(tagName);
        if (nodes.getLength() == 0 || nodes.item(0) == null) {
            return "";
        }
        String value = nodes.item(0).getTextContent();
        return value == null ? "" : value.trim();
    }

    private String buildDestinatariosHtml(Document document) {
        NodeList destinatarios = document.getElementsByTagName("destinatario");
        if (destinatarios.getLength() == 0) {
            return "<p class=\"muted\">No se encontraron destinatarios en la guia.</p>";
        }

        List<String> blocks = new ArrayList<>();
        for (int i = 0; i < destinatarios.getLength(); i++) {
            Node node = destinatarios.item(i);
            if (node instanceof Element destinatario) {
                blocks.add("""
                        <div class="card">
                          <table class="grid">
                            <tr><th>Destinatario</th><td>%s</td><th>Identificacion</th><td>%s</td></tr>
                            <tr><th>Direccion</th><td>%s</td><th>Motivo</th><td>%s</td></tr>
                            <tr><th>Ruta</th><td>%s</td><th>Doc. sustento</th><td>%s</td></tr>
                          </table>
                          <table class="grid">
                            <tr>
                              <th>Codigo</th>
                              <th>Codigo adicional</th>
                              <th>Descripcion</th>
                              <th>Cantidad</th>
                            </tr>
                            %s
                          </table>
                        </div>
                        """.formatted(
                        escape(childText(destinatario, "razonSocialDestinatario")),
                        escape(childText(destinatario, "identificacionDestinatario")),
                        escape(childText(destinatario, "dirDestinatario")),
                        escape(childText(destinatario, "motivoTraslado")),
                        escape(childText(destinatario, "ruta")),
                        escape(childText(destinatario, "numDocSustento")),
                        buildDetallesHtml(destinatario)
                ));
            }
        }
        return String.join("", blocks);
    }

    private String buildDetallesHtml(Element destinatario) {
        NodeList detalles = destinatario.getElementsByTagName("detalle");
        if (detalles.getLength() == 0) {
            return "<tr><td colspan=\"4\">Sin detalles</td></tr>";
        }

        List<String> rows = new ArrayList<>();
        for (int i = 0; i < detalles.getLength(); i++) {
            Node node = detalles.item(i);
            if (node instanceof Element detalle) {
                rows.add("""
                        <tr>
                          <td>%s</td>
                          <td>%s</td>
                          <td>%s</td>
                          <td>%s</td>
                        </tr>
                        """.formatted(
                        escape(childText(detalle, "codigoInterno")),
                        escape(childText(detalle, "codigoAdicional")),
                        escape(childText(detalle, "descripcion")),
                        escape(childText(detalle, "cantidad"))
                ));
            }
        }
        return String.join("", rows);
    }

    private String childText(Element parent, String tagName) {
        NodeList children = parent.getElementsByTagName(tagName);
        if (children.getLength() == 0 || children.item(0) == null) {
            return "";
        }
        String value = children.item(0).getTextContent();
        return value == null ? "" : value.trim();
    }

    private byte[] generarNotaComercialDesdeXml(
            String xml,
            String titulo,
            String rootTagName,
            String infoTagName,
            String totalTagName,
            String motivoTagName
    ) {
        try {
            Document document = parse(xml);
            Element root = firstElement(document, rootTagName);
            Element info = firstElement(document, infoTagName);
            String html = """
                    <html>
                      <head>
                        <meta charset="UTF-8" />
                        <style>
                          body { font-family: Arial, sans-serif; padding: 28px; color: #1f2937; font-size: 11px; }
                          h1 { font-size: 20px; margin: 0 0 8px 0; }
                          h2 { font-size: 14px; margin: 18px 0 8px 0; color: #1d4ed8; }
                          .subtitle { margin: 0 0 18px 0; color: #4b5563; }
                          .grid { width: 100%%; border-collapse: collapse; margin-bottom: 12px; }
                          .grid td, .grid th { border: 1px solid #d1d5db; padding: 6px 8px; vertical-align: top; }
                          .grid th { background: #eff6ff; text-align: left; }
                          .note { border: 1px solid #bfdbfe; background: #f8fbff; border-radius: 8px; padding: 12px; }
                        </style>
                      </head>
                      <body>
                        <h1>%s</h1>
                        <p class="subtitle">Representacion PDF basica del comprobante autorizado.</p>
                        <h2>Informacion tributaria</h2>
                        <table class="grid">
                          <tr><th>Clave de acceso</th><td>%s</td><th>RUC</th><td>%s</td></tr>
                          <tr><th>Emisor</th><td>%s</td><th>Documento</th><td>%s-%s-%s</td></tr>
                          <tr><th>Ambiente</th><td>%s</td><th>Direccion matriz</th><td>%s</td></tr>
                        </table>
                        <h2>Informacion comercial</h2>
                        <table class="grid">
                          <tr><th>Cliente</th><td>%s</td><th>Identificacion</th><td>%s</td></tr>
                          <tr><th>Documento modificado</th><td>%s</td><th>Fecha sustento</th><td>%s</td></tr>
                          <tr><th>Total sin impuestos</th><td>%s</td><th>Total documento</th><td>%s</td></tr>
                        </table>
                        <div class="note">
                          <strong>Motivo:</strong> %s
                        </div>
                      </body>
                    </html>
                    """.formatted(
                    escape(titulo),
                    escape(text(document, "claveAcceso")),
                    escape(text(document, "ruc")),
                    escape(text(document, "razonSocial")),
                    escape(text(document, "estab")),
                    escape(text(document, "ptoEmi")),
                    escape(text(document, "secuencial")),
                    escape(text(document, "ambiente")),
                    escape(text(document, "dirMatriz")),
                    escape(childText(info, "razonSocialComprador")),
                    escape(childText(info, "identificacionComprador")),
                    escape(childText(info, "numDocModificado")),
                    escape(childText(info, "fechaEmisionDocSustento")),
                    escape(childText(info, "totalSinImpuestos")),
                    escape(childText(info, totalTagName)),
                    escape(firstNonBlank(
                            childText(info, motivoTagName),
                            childText(root, motivoTagName),
                            text(document, motivoTagName)
                    ))
            );

            ByteArrayOutputStream output = new ByteArrayOutputStream();
            HtmlConverter.convertToPdf(html, output);
            return output.toByteArray();
        } catch (Exception ex) {
            return generarDesdeXml(titulo, xml);
        }
    }

    private Element firstElement(Document document, String tagName) {
        NodeList nodes = document.getElementsByTagName(tagName);
        if (nodes.getLength() == 0 || !(nodes.item(0) instanceof Element element)) {
            throw new IllegalArgumentException("Tag no encontrado: " + tagName);
        }
        return element;
    }

    private String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value.trim();
            }
        }
        return "";
    }
}
