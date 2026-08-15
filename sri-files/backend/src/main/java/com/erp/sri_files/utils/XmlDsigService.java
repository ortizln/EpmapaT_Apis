package com.erp.sri_files.utils;

import org.apache.jcp.xml.dsig.internal.dom.XMLDSigRI;
import org.springframework.stereotype.Service;
import org.w3c.dom.*;
import org.xml.sax.InputSource;

import javax.xml.XMLConstants;
import javax.xml.crypto.dsig.*;
import javax.xml.crypto.dsig.dom.DOMSignContext;
import javax.xml.crypto.dsig.keyinfo.*;
import javax.xml.crypto.dsig.spec.C14NMethodParameterSpec;
import javax.xml.crypto.dsig.spec.TransformParameterSpec;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.*;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.StringReader;
import java.io.StringWriter;
import java.security.Security;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.util.*;
@Service
public class XmlDsigService {

    public XmlDsigService() {
        Security.addProvider(new XMLDSigRI());
    }

    /** Firma ENVELOPED el nodo id="comprobante" con RSA-SHA256 e inserta KeyInfo con la cadena. */
    public String signFacturaComprobante(String xmlContent, Pkcs12Loader.KeyMaterial km) throws Exception {
        if (xmlContent == null || xmlContent.isBlank()) throw new IllegalArgumentException("XML vacío");

        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        dbf.setNamespaceAware(true);
        dbf.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
        dbf.setFeature("http://xml.org/sax/features/external-general-entities", false);
        dbf.setFeature("http://xml.org/sax/features/external-parameter-entities", false);

        Document doc = dbf.newDocumentBuilder().parse(new InputSource(new StringReader(xmlContent)));
        Element root = doc.getDocumentElement();

        // MUY IMPORTANTE: declarar el atributo "id" como tipo ID para resolver #comprobante
        root.setIdAttribute("id", true);

        XMLSignatureFactory fac = XMLSignatureFactory.getInstance("DOM");

        List<Transform> transforms = new ArrayList<>();
        transforms.add(fac.newTransform(Transform.ENVELOPED, (TransformParameterSpec) null));
        transforms.add(fac.newTransform("http://www.w3.org/2001/10/xml-exc-c14n#", (TransformParameterSpec) null));

        Reference ref = fac.newReference(
                "#comprobante",
                fac.newDigestMethod(DigestMethod.SHA256, null),
                transforms, null, null
        );

        SignedInfo si = fac.newSignedInfo(
                fac.newCanonicalizationMethod(CanonicalizationMethod.INCLUSIVE, (C14NMethodParameterSpec) null),
                fac.newSignatureMethod(SignatureMethod.RSA_SHA256, null),
                Collections.singletonList(ref)
        );

        // KeyInfo con cadena completa (si existe)
        KeyInfoFactory kif = fac.getKeyInfoFactory();
        List<Object> x509Content = new ArrayList<>();
        Certificate[] chain = km.chain();
        if (chain != null && chain.length > 0) {
            for (Certificate c : chain) x509Content.add((X509Certificate) c);
        } else {
            x509Content.add(km.cert());
        }
        X509Data x509Data = kif.newX509Data(x509Content);
        KeyInfo ki = kif.newKeyInfo(Collections.singletonList(x509Data));

        DOMSignContext dsc = new DOMSignContext(km.key(), root);
        dsc.setDefaultNamespacePrefix("ds");

        XMLSignature signature = fac.newXMLSignature(si, ki);
        signature.sign(dsc);

        TransformerFactory tf = TransformerFactory.newInstance();
        try {
            tf.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");
            tf.setAttribute(XMLConstants.ACCESS_EXTERNAL_STYLESHEET, "");
        } catch (IllegalArgumentException ignore) {}
        Transformer t = tf.newTransformer();
        t.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
        t.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "no");
        t.setOutputProperty(OutputKeys.INDENT, "no");
        StringWriter sw = new StringWriter();
        t.transform(new DOMSource(doc), new StreamResult(sw));
        return sw.toString().replace("&#13;", "");
    }
}
