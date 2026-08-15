package com.erp.sri_files.processor;

import com.erp.sri_files.domain.documento.TipoDocumento;
import org.springframework.stereotype.Component;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

@Component
public class DocumentoProcessorFactory {

    private final Map<TipoDocumento, DocumentoProcessor> processors;

    public DocumentoProcessorFactory(List<DocumentoProcessor> processorList) {
        this.processors = new EnumMap<>(TipoDocumento.class);
        for (DocumentoProcessor processor : processorList) {
            this.processors.put(processor.soporta(), processor);
        }
    }

    public DocumentoProcessor obtener(TipoDocumento tipoDocumento) {
        DocumentoProcessor processor = processors.get(tipoDocumento);
        if (processor == null) {
            throw new IllegalArgumentException("No existe processor para " + tipoDocumento);
        }
        return processor;
    }
}
