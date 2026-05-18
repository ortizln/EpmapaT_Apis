package com.erp.sri_files.utils;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.util.Base64;
import java.util.Enumeration;

@Service
public class Pkcs12Loader {
    public record KeyMaterial(PrivateKey key, X509Certificate cert, Certificate[] chain) {}

    public static byte[] normalizeBytes(byte[] fromDb, boolean base64) {
        if (!base64) return fromDb;
        return Base64.getDecoder().decode(fromDb);
    }

    /** Abre el PKCS12 probando pass dada y pass vacía (keystore y clave privada). */
    public static KeyMaterial load(byte[] pkcs12Bytes, char[] password) throws Exception {
        if (pkcs12Bytes == null || pkcs12Bytes.length == 0)
            throw new IllegalArgumentException("PKCS12 vacío");

        KeyStore ks = KeyStore.getInstance("PKCS12");
        Exception e1 = null;
        try {
            ks.load(new ByteArrayInputStream(pkcs12Bytes), password);
        } catch (Exception ex) {
            e1 = ex;
            // Intentar keystore con pass vacía
            ks.load(new ByteArrayInputStream(pkcs12Bytes), new char[0]);
        }

        String alias = null;
        for (Enumeration<String> e = ks.aliases(); e.hasMoreElements();) {
            String a = e.nextElement();
            if (ks.isKeyEntry(a)) { alias = a; break; }
        }
        if (alias == null) throw new IllegalStateException("El PKCS12 no contiene PrivateKeyEntry");

        PrivateKey key;
        try {
            key = (PrivateKey) ks.getKey(alias, password);
        } catch (Exception ex) {
            key = (PrivateKey) ks.getKey(alias, new char[0]);
            if (key == null) throw new IllegalStateException("Contraseña de la clave privada incorrecta");
        }

        X509Certificate cert = (X509Certificate) ks.getCertificate(alias);
        Certificate[] chain = ks.getCertificateChain(alias);
        return new KeyMaterial(key, cert, chain);
    }

    private Pkcs12Loader(){}
}
