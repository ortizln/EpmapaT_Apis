package com.erp.sri_files.config;

import java.util.Base64;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;

public class AESUtil {
    private static final String CLAVE_SECRETA = "1234567890123456"; // 16 caracteres (AES-128)
//Cifado de datos
    public static String cifrar(String datos) throws Exception {
        SecretKeySpec key = new SecretKeySpec(CLAVE_SECRETA.getBytes(), "AES");
        Cipher cipher = Cipher.getInstance("AES");
        cipher.init(Cipher.ENCRYPT_MODE, key);
        byte[] cifrado = cipher.doFinal(datos.getBytes());
        return Base64.getEncoder().encodeToString(cifrado);
    }

    public static String descifrar(String datosCifrados) throws Exception {
        SecretKeySpec key = new SecretKeySpec(CLAVE_SECRETA.getBytes(), "AES");
        Cipher cipher = Cipher.getInstance("AES");
        cipher.init(Cipher.DECRYPT_MODE, key);
        byte[] descifrado = cipher.doFinal(Base64.getDecoder().decode(datosCifrados));
        return new String(descifrado);
    }

}