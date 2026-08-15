package com.erp.sri_files.services;

import com.erp.sri_files.dto.AttachmentDTO;
import com.erp.sri_files.dto.SendMailRequest;
import com.erp.sri_files.dto.TemplateMailRequest;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

@Service
public class MailService {

    private static final int MAX_TOTAL_ATTACHMENTS_MB = 20;

    private final EmailMsClientService emailMsClientService;
    private final TemplateEngine templateEngine;

    public MailService(EmailMsClientService emailMsClientService, TemplateEngine templateEngine) {
        this.emailMsClientService = emailMsClientService;
        this.templateEngine = templateEngine;
    }

    @Retryable(maxAttempts = 3, backoff = @Backoff(delay = 1500))
    public java.util.UUID send(SendMailRequest req) throws Exception {
        return send(req, null);
    }

    @Retryable(maxAttempts = 3, backoff = @Backoff(delay = 1500))
    public java.util.UUID send(SendMailRequest req, String correlationId) throws Exception {
        validateAttachments(req.attachments());
        return emailMsClientService.enqueueDocumentEmail(req, correlationId);
    }

    @Retryable(maxAttempts = 3, backoff = @Backoff(delay = 1500))
    public boolean sendMail(SendMailRequest req) {
        return sendMail(req, null);
    }

    @Retryable(maxAttempts = 3, backoff = @Backoff(delay = 1500))
    public boolean sendMail(SendMailRequest req, String correlationId) {
        try {
            send(req, correlationId);
            return true;
        } catch (Exception ex) {
            return false;
        }
    }

    @Retryable(maxAttempts = 3, backoff = @Backoff(delay = 1500))
    public void sendTemplate(TemplateMailRequest req) throws Exception {
        Context ctx = new Context(java.util.Locale.getDefault());
        if (req.model() != null) {
            req.model().forEach(ctx::setVariable);
        }

        String html = templateEngine.process(req.template(), ctx);
        SendMailRequest base = new SendMailRequest(
                req.from(),
                req.to(),
                req.cc(),
                req.bcc(),
                req.subject(),
                html,
                req.attachments(),
                null
        );
        send(base);
    }

    private void validateAttachments(java.util.List<AttachmentDTO> atts) {
        if (atts == null || atts.isEmpty()) {
            return;
        }

        long totalBytes = 0L;
        for (AttachmentDTO a : atts) {
            byte[] data = java.util.Base64.getDecoder().decode(a.base64());
            totalBytes += data.length;
        }

        long totalMB = totalBytes / (1024 * 1024);
        if (totalMB > MAX_TOTAL_ATTACHMENTS_MB) {
            throw new IllegalArgumentException("Adjuntos superan " + MAX_TOTAL_ATTACHMENTS_MB + "MB");
        }
    }

    public boolean smtpHealth() {
        return emailMsClientService.health();
    }
}
