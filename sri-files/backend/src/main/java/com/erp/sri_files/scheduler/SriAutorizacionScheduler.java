package com.erp.sri_files.scheduler;

import com.erp.sri_files.services.EnvioSriBatchService;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Scheduler de consulta de autorización / recuperación de XML para
 * comprobantes en estado C/O (pipeline legacy).
 */
@Component
@ConditionalOnProperty(prefix = "sri.legacy-scheduler", name = "enabled", havingValue = "true")
public class SriAutorizacionScheduler {

    private final EnvioSriBatchService envioSriBatchService;

    public SriAutorizacionScheduler(EnvioSriBatchService envioSriBatchService) {
        this.envioSriBatchService = envioSriBatchService;
    }

    @Scheduled(cron = "${sri.scheduler.recuperacion-xml.cron}")
    public void consultarAutorizaciones() {
        envioSriBatchService.automatizacionConsultarXml();
    }
}