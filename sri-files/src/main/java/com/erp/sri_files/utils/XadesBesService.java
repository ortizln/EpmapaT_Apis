package com.erp.sri_files.utils;

import org.apache.jcp.xml.dsig.internal.dom.XMLDSigRI;
import org.springframework.stereotype.Service;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.InputSource;

import xades4j.algorithms.GenericAlgorithm;
import xades4j.production.*;
import xades4j.properties.DataObjectDesc;
import xades4j.properties.DataObjectFormatProperty;
import xades4j.providers.impl.DirectKeyingDataProvider;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;
import java.io.ByteArrayInputStream;
import java.io.StringReader;
import java.io.StringWriter;
import java.security.Security;
@Service
public class XadesBesService {

    public XadesBesService() {
        Security.addProvider(new XMLDSigRI());
        System.setProperty("org.apache.xml.security.ignoreLineBreaks", "true");
        try { org.apache.xml.security.Init.init(); } catch (Throwable ignored) {}
    }

    /** Firma ENVELOPED XAdES-BES cualquier comprobante SRI (factura, retención, etc.) */
    public String signComprobanteXades(byte[] xmlBytes, Pkcs12Loader.KeyMaterial km) throws Exception {
        if (xmlBytes == null || xmlBytes.length == 0)
            throw new IllegalArgumentException("XML vacío");

        // Parser seguro
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        dbf.setNamespaceAware(true);
        dbf.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
        dbf.setFeature("http://xml.org/sax/features/external-general-entities", false);
        dbf.setFeature("http://xml.org/sax/features/external-parameter-entities", false);

        // 🔑 Lee directo de bytes → respeta encoding / BOM
        InputSource is = new InputSource(new ByteArrayInputStream(xmlBytes));
        Document doc = dbf.newDocumentBuilder().parse(is);
        Element root = doc.getDocumentElement();

        if (!root.hasAttribute("id")) {
            throw new IllegalStateException("El elemento raíz no tiene atributo id=\"comprobante\"");
        }
        root.setIdAttribute("id", true);

        // Proveedor directo (cert + key) desde el P12
        DirectKeyingDataProvider kdp = new DirectKeyingDataProvider(km.cert(), km.key());
        XadesSigner signer = (new XadesBesSigningProfile(kdp)).newSigner();

        // Referencia con transforms: enveloped + exclusive-c14n
        DataObjectDesc obj = new DataObjectReference("#comprobante")
                .withTransform(new GenericAlgorithm("http://www.w3.org/2000/09/xmldsig#enveloped-signature"))
                .withTransform(new GenericAlgorithm("http://www.w3.org/2001/10/xml-exc-c14n#"));

        // 4) SignedDataObjects
        SignedDataObjects sdos = new SignedDataObjects(obj);

        // 5) **Firmar directo con el signer** (sin Enveloped)
        signer.sign(sdos, root);   // <-- esta es la API que tu jar expone

        // Serializa sin &#13;
        Transformer t = TransformerFactory.newInstance().newTransformer();
        StringWriter sw = new StringWriter();
        t.transform(new DOMSource(doc), new StreamResult(sw));
        return sw.toString().replace("&#13;", "");
    }
}
