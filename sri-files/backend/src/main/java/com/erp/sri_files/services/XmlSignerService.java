package com.erp.sri_files.services;

import org.apache.jcp.xml.dsig.internal.dom.XMLDSigRI;
import org.springframework.stereotype.Service;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.InputSource;

import javax.xml.XMLConstants;
import javax.xml.crypto.dsig.*;
import javax.xml.crypto.dsig.dom.DOMSignContext;
import javax.xml.crypto.dsig.keyinfo.KeyInfo;
import javax.xml.crypto.dsig.keyinfo.KeyInfoFactory;
import javax.xml.crypto.dsig.keyinfo.X509Data;
import javax.xml.crypto.dsig.spec.C14NMethodParameterSpec;
import javax.xml.crypto.dsig.spec.TransformParameterSpec;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.ByteArrayInputStream;
import java.io.StringReader;
import java.io.StringWriter;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.Security;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.util.*;

@Service
public class XmlSignerService {

    /**
     * Firma XML (enveloped) con RSA-SHA256 y X509Data en KeyInfo.
     *
     * @param xmlContent      XML a firmar (String)
     * @param pkcs12Bytes     bytes del .p12/.pfx (NO Base64). Si lo tienes en Base64, decodifica antes.
     * @param password        contraseña (keystore y/o clave privada). Si tu keystore tiene pass vacía,
     *                        pasa "" y el método probará ambas combinaciones.
     */
    public String signXml(String xmlContent, byte[] pkcs12Bytes, String password) throws Exception {
        if (pkcs12Bytes == null || pkcs12Bytes.length == 0)
            throw new IllegalArgumentException("Certificado PKCS#12 vacío");
        if (xmlContent == null || xmlContent.trim().isEmpty())
            throw new IllegalArgumentException("XML vacío");
        if (password == null)
            password = ""; // mejor tratar como vacía que null

        // --- Cargar el PKCS12 (tolerante a pass vacía en keystore) ---
        KeyMaterial km = loadPkcs12(pkcs12Bytes, password.toCharArray());

        // --- Seguridad y proveedor (XMLDSig) ---
        Security.addProvider(new XMLDSigRI());
        System.setProperty("org.apache.xml.security.ignoreLineBreaks", "true");

        // --- Parsear XML (XXE hardened) ---
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        dbf.setNamespaceAware(true);
        dbf.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
        dbf.setFeature("http://xml.org/sax/features/external-general-entities", false);
        dbf.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
        Document doc = dbf.newDocumentBuilder()
                .parse(new InputSource(new StringReader(xmlContent)));
        Element root = doc.getDocumentElement();

        // --- Fábrica de firma ---
        XMLSignatureFactory fac = XMLSignatureFactory.getInstance("DOM");

        // Transformadas: enveloped + exclusive c14n
        List<Transform> transforms = new ArrayList<>();
        transforms.add(fac.newTransform(Transform.ENVELOPED, (TransformParameterSpec) null));
        transforms.add(fac.newTransform("http://www.w3.org/2001/10/xml-exc-c14n#", (TransformParameterSpec) null));

        // Referencia al documento completo ("")
        Reference ref = fac.newReference(
                "",
                fac.newDigestMethod(DigestMethod.SHA256, null),
                transforms,
                null,
                null
        );

        // SignedInfo: c14n inclusiva (o exclusive si prefieres) + RSA-SHA256
        SignedInfo si = fac.newSignedInfo(
                fac.newCanonicalizationMethod(CanonicalizationMethod.INCLUSIVE, (C14NMethodParameterSpec) null),
                fac.newSignatureMethod(SignatureMethod.RSA_SHA256, null),
                Collections.singletonList(ref)
        );

        // KeyInfo: X509Data (cert o cadena)
        KeyInfoFactory kif = fac.getKeyInfoFactory();
        List<Object> x509Content = new ArrayList<>();
        if (km.chain != null && km.chain.length > 0) {
            for (Certificate c : km.chain) x509Content.add((X509Certificate) c);
        } else {
            x509Content.add(km.cert);
        }
        X509Data x509Data = kif.newX509Data(x509Content);
        KeyInfo ki = kif.newKeyInfo(Collections.singletonList(x509Data));

        // Firmar (inserta <ds:Signature> dentro del root)
        DOMSignContext dsc = new DOMSignContext(km.privateKey, root);
        dsc.setDefaultNamespacePrefix("ds");
        XMLSignature signature = fac.newXMLSignature(si, ki);
        signature.sign(dsc);

        // Serializar resultante
        TransformerFactory tf = TransformerFactory.newInstance();
        try {
            tf.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");
            tf.setAttribute(XMLConstants.ACCESS_EXTERNAL_STYLESHEET, "");
        } catch (IllegalArgumentException ignored) {}
        Transformer t = tf.newTransformer();
        t.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "no");
        t.setOutputProperty(OutputKeys.INDENT, "no");
        t.setOutputProperty(OutputKeys.ENCODING, "UTF-8");

        StringWriter sw = new StringWriter();
        t.transform(new DOMSource(doc), new StreamResult(sw));
        // algunos validadores detestan los CR como entidades:
        return sw.toString().replace("&#13;", "");
    }

    /** Carga PKCS#12 probando: (a) pass del keystore; (b) keystore con pass vacía. También prueba la clave. */
    private static KeyMaterial loadPkcs12(byte[] pkcs12, char[] pass) throws Exception {
        Exception loadErr1 = null, loadErr2 = null;
        KeyStore ks = KeyStore.getInstance("PKCS12");

        // 1) intentar con la contraseña provista
        try {
            ks.load(new ByteArrayInputStream(pkcs12), pass);
        } catch (Exception e) {
            loadErr1 = e;
            // 2) intentar keystore con pass vacía (muy común)
            try {
                ks.load(new ByteArrayInputStream(pkcs12), new char[0]);
            } catch (Exception e2) {
                loadErr2 = e2;
                throw new IllegalStateException("No se pudo abrir el PKCS12 (keystore): contraseña incorrecta o archivo corrupto");
            }
        }

        // elegir alias con KeyEntry
        String alias = null;
        for (Enumeration<String> en = ks.aliases(); en.hasMoreElements();) {
            String a = en.nextElement();
            if (ks.isKeyEntry(a)) { alias = a; break; }
        }
        if (alias == null) throw new IllegalStateException("El PKCS12 no contiene PrivateKeyEntry");

        // obtener PrivateKey probando pass dada y pass vacía
        PrivateKey key = null;
        try {
            key = (PrivateKey) ks.getKey(alias, pass);
        } catch (Exception ignored) {
            try {
                key = (PrivateKey) ks.getKey(alias, new char[0]);
            } catch (Exception e) {
                throw new IllegalStateException("La contraseña de la clave privada es incorrecta");
            }
        }
        X509Certificate cert = (X509Certificate) ks.getCertificate(alias);
        Certificate[] chain = ks.getCertificateChain(alias);

        return new KeyMaterial(key, cert, chain);
    }

    private static final class KeyMaterial {
        final PrivateKey privateKey;
        final X509Certificate cert;
        final Certificate[] chain;
        KeyMaterial(PrivateKey k, X509Certificate c, Certificate[] ch) { this.privateKey = k; this.cert = c; this.chain = ch; }
    }
}
