package com.erp.sri_files.config;

import xades4j.providers.KeyingDataProvider;
import xades4j.providers.SigningCertChainException;
import xades4j.providers.SigningKeyException;
import xades4j.verification.UnexpectedJCAException;

import java.security.KeyStore.PrivateKeyEntry;
import java.security.PrivateKey;
import java.security.cert.X509Certificate;
import java.util.List;

public class SingleKeyStoreKeyingDataProvider implements KeyingDataProvider {
    private final PrivateKeyEntry privateKeyEntry;

    public SingleKeyStoreKeyingDataProvider(PrivateKeyEntry privateKeyEntry) {
        this.privateKeyEntry = privateKeyEntry;
    }

    @Override
    public List<X509Certificate> getSigningCertificateChain() throws SigningCertChainException, UnexpectedJCAException {
        return List.of();
    }

    @Override
    public PrivateKey getSigningKey(X509Certificate x509Certificate) throws SigningKeyException, UnexpectedJCAException {
        return null;
    }

}
