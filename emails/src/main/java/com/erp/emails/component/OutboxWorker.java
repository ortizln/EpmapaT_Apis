package com.erp.emails.component;

import com.erp.emails.model.EmailStatus;
import com.erp.emails.repository.EmailMessageR;
import com.erp.emails.service.EmailProcessorService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class OutboxWorker {

    private final EmailMessageR emailRepo;
    private final EmailProcessorService emailProcessorService;

    @Value("${mailms.outbox.batch-size:20}")
    private int batchSize;

    @Value("${mailms.outbox.max-attempts:5}")
    private int maxAttempts;

    public OutboxWorker(EmailMessageR emailRepo, EmailProcessorService emailProcessorService) {
        this.emailRepo = emailRepo;
        this.emailProcessorService = emailProcessorService;
    }

    @Scheduled(fixedDelayString = "${mailms.outbox.scheduler-fixed-delay-ms:15000}")
    public void process() {
        var ids = emailRepo.findNextPendingIds(
                EmailStatus.PENDING,
                maxAttempts,
                PageRequest.of(0, batchSize)
        );

        for (var emailId : ids) {
            try {
                emailProcessorService.procesarEmail(emailId, maxAttempts);
            } catch (Exception e) {
                System.err.println("Error procesando email ID: " + emailId);
                e.printStackTrace();
            }
        }
    }
}
