package com.erp.sri_files.service;

import com.erp.sri_files.domain.documento.TipoDocumento;
import com.erp.sri_files.exceptions.DocumentoRecepcionException;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.util.Map;

@Service
public class RideTemplateCatalogService {

    private static final Map<TipoDocumento, String> BASE_TEMPLATES = Map.of(
            TipoDocumento.FACTURA, "reports/templates/base_factura.jrxml",
            TipoDocumento.RETENCION, "reports/templates/base_retencion.jrxml",
            TipoDocumento.NOTA_CREDITO, "reports/templates/base_nota_credito.jrxml",
            TipoDocumento.NOTA_DEBITO, "reports/templates/base_nota_debito.jrxml",
            TipoDocumento.GUIA_REMISION, "reports/templates/base_guia_remision.jrxml",
            TipoDocumento.LIQUIDACION_COMPRA, "reports/templates/base_liquidacion_compra.jrxml"
    );

    public byte[] descargarBase(TipoDocumento tipoDocumento) {
        String resourcePath = BASE_TEMPLATES.get(tipoDocumento);
        if (resourcePath == null) {
            throw new DocumentoRecepcionException("No existe plantilla base para el tipo " + tipoDocumento.name());
        }
        try (InputStream inputStream = new ClassPathResource(resourcePath).getInputStream()) {
            return inputStream.readAllBytes();
        } catch (IOException ex) {
            throw new DocumentoRecepcionException("No fue posible leer la plantilla base de " + tipoDocumento.name());
        }
    }

    public String nombreArchivo(TipoDocumento tipoDocumento) {
        return "base_" + tipoDocumento.name().toLowerCase() + ".jrxml";
    }
}
