package com.erp.sri_files.scheduler;

import com.erp.sri_files.services.EnvioSriBatchService;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Scheduler de recuperación de bloqueos abandonados (facturas en estado P
 * cuyo worker dejó de estar activo).
 */
@Component
@ConditionalOnProperty(prefix = "sri.legacy-scheduler", name = "enabled", havingValue = "true")
public class SriRecuperacionBloqueosScheduler {

    private final EnvioSriBatchService envioSriBatchService;

    public SriRecuperacionBloqueosScheduler(EnvioSriBatchService envioSriBatchService) {
        this.envioSriBatchService = envioSriBatchService;
    }

    @Scheduled(cron = "${sri.scheduler.recuperacion-bloqueos.cron}")
    public void recuperarBloqueos() {
        envioSriBatchService.recuperarBloqueosAbandonados();
    }
}