package com.erp.sri_files.domain.documento;

public enum TipoDocumento {
    FACTURA("01"),
    LIQUIDACION_COMPRA("03"),
    NOTA_CREDITO("04"),
    NOTA_DEBITO("05"),
    GUIA_REMISION("06"),
    RETENCION("07");

    private final String codigoSri;

    TipoDocumento(String codigoSri) {
        this.codigoSri = codigoSri;
    }

    public String getCodigoSri() {
        return codigoSri;
    }
}
