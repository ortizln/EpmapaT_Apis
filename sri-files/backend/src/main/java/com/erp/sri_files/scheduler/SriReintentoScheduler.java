package com.erp.sri_files.scheduler;

import com.erp.sri_files.services.EnvioSriBatchService;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Scheduler del motor de reintentos programados: procesa los documentos
 * cuyo retry venció y cierra los que agotaron el máximo de intentos.
 * No-op salvo que sri.retry.enabled=true.
 */
@Component
@ConditionalOnProperty(prefix = "sri.legacy-scheduler", name = "enabled", havingValue = "true")
public class SriReintentoScheduler {

    private final EnvioSriBatchService envioSriBatchService;

    public SriReintentoScheduler(EnvioSriBatchService envioSriBatchService) {
        this.envioSriBatchService = envioSriBatchService;
    }

    @Scheduled(cron = "${sri.scheduler.reintentos.cron}")
    public void reintentosProgramados() {
        envioSriBatchService.automatizacionReintentosProgramados();
    }
}