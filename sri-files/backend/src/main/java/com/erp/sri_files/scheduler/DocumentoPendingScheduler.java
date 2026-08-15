package com.erp.sri_files.scheduler;

import com.erp.sri_files.service.DocumentoProcessingService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class DocumentoPendingScheduler {

    private static final Logger log = LoggerFactory.getLogger(DocumentoPendingScheduler.class);

    private final DocumentoProcessingService documentoProcessingService;

    public DocumentoPendingScheduler(DocumentoProcessingService documentoProcessingService) {
        this.documentoProcessingService = documentoProcessingService;
    }

    @Scheduled(cron = "${sri-files.processing.scheduler.recibidos-cron:0 */1 * * * *}")
    public void procesarRecibidos() {
        int procesados = documentoProcessingService.procesarPendientesRecibidos();
        if (procesados > 0) {
            log.info("Worker documento_recibido proceso {} documento(s)", procesados);
        }
    }
}
