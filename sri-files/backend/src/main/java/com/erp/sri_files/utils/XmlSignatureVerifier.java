package com.erp.sri_files.utils;

import org.springframework.stereotype.Service;

import javax.xml.crypto.AlgorithmMethod;
import javax.xml.crypto.KeySelector;
import javax.xml.crypto.KeySelectorException;
import javax.xml.crypto.KeySelectorResult;
import javax.xml.crypto.XMLCryptoContext;
import javax.xml.crypto.dsig.keyinfo.KeyInfo;
import javax.xml.crypto.dsig.keyinfo.X509Data;
import java.security.Key;
import java.security.cert.X509Certificate;

/**
 * Selector sencillo que toma la PublicKey del primer X509Certificate encontrado en KeyInfo.
 */
@Service
public final class XmlSignatureVerifier {

    public static class KeyValueKeySelector extends KeySelector {
        @Override
        public KeySelectorResult select(KeyInfo keyInfo,
                                        Purpose purpose,
                                        AlgorithmMethod method,
                                        XMLCryptoContext context)
                throws KeySelectorException {
            if (keyInfo == null) {
                throw new KeySelectorException("KeyInfo nulo");
            }

            for (Object kiObj : keyInfo.getContent()) {
                if (kiObj instanceof X509Data x509Data) {
                    for (Object dataObj : x509Data.getContent()) {
                        if (dataObj instanceof X509Certificate cert) {
                            final Key pubKey = cert.getPublicKey();
                            return () -> pubKey; // implementación inline de KeySelectorResult
                        }
                    }
                }
            }

            throw new KeySelectorException("No se encontró X509Certificate en KeyInfo");
        }
    }
}