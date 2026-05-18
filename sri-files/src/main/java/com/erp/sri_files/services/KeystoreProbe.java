package com.erp.sri_files.services;

import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.cert.X509Certificate;
import java.util.Base64;
import java.util.Enumeration;
@Service
public final class KeystoreProbe {

    public static Result probePkcs12(Object firmaField, String password) throws Exception {
        byte[] p12 = toBytes(firmaField); // soporta byte[] o Base64 String
        char[] pass = password != null ? password.toCharArray() : null;

        KeyStore ks = KeyStore.getInstance("PKCS12");

        // 1) Intento con pass
        try {
            ks.load(new ByteArrayInputStream(p12), pass);
        } catch (Exception e1) {
            // 2) Intento con contraseña vacía (algunos PKCS12 tienen keystore password vacío)
            try {
                ks.load(new ByteArrayInputStream(p12), new char[0]);
            } catch (Exception e2) {
                throw new IllegalStateException("No se pudo abrir el PKCS12 (keystore): contraseña incorrecta o archivo corrupto");
            }
        }

        String aliasPrivado = null;
        for (Enumeration<String> e = ks.aliases(); e.hasMoreElements(); ) {
            String a = e.nextElement();
            if (ks.isKeyEntry(a)) {
                aliasPrivado = a;
                break;
            }
        }
        if (aliasPrivado == null) {
            throw new IllegalStateException("El PKCS12 no contiene entrada de clave privada (PrivateKeyEntry)");
        }

        // 3) Intentar obtener la clave privada con pass; si falla, con vacío
        PrivateKey key = null;
        try {
            key = (PrivateKey) ks.getKey(aliasPrivado, pass);
        } catch (Exception ignore) {
            try {
                key = (PrivateKey) ks.getKey(aliasPrivado, new char[0]);
            } catch (Exception e) {
                throw new IllegalStateException("La contraseña de la CLAVE PRIVADA es incorrecta");
            }
        }
        if (key == null) throw new IllegalStateException("No se pudo obtener la clave privada del PKCS12");

        X509Certificate cert = (X509Certificate) ks.getCertificate(aliasPrivado);
        return new Result(ks, aliasPrivado, key, cert);
    }

    private static byte[] toBytes(Object firmaField) {
        if (firmaField instanceof byte[]) return (byte[]) firmaField;
        if (firmaField instanceof String s) {
            // Intentar decodificar Base64; si no es Base64 válido, asumir que es XML (!)
            try { return Base64.getDecoder().decode(s); } catch (IllegalArgumentException ex) { /* fall-through */ }
        }
        throw new IllegalArgumentException("Campo firma no es byte[] ni Base64 String válido");
    }

    public record Result(KeyStore ks, String alias, PrivateKey key, X509Certificate cert) {}
}
