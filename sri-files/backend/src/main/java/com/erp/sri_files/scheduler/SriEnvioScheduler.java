package com.erp.sri_files.scheduler;

import com.erp.sri_files.services.EnvioSriBatchService;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Scheduler del envío de facturas al SRI (pipeline legacy).
 * Solo se activa si sri.legacy-scheduler.enabled=true.
 */
@Component
@ConditionalOnProperty(prefix = "sri.legacy-scheduler", name = "enabled", havingValue = "true")
public class SriEnvioScheduler {

    private final EnvioSriBatchService envioSriBatchService;

    public SriEnvioScheduler(EnvioSriBatchService envioSriBatchService) {
        this.envioSriBatchService = envioSriBatchService;
    }

    @Scheduled(cron = "${sri.scheduler.envio-facturas.cron}")
    public void enviarFacturas() {
        envioSriBatchService.automatizacionEnvioFacturasElectonicas();
    }
}