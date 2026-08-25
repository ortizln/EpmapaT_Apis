package com.erp.sri_files.services;

import com.erp.sri_files.dto.AttachmentDTO;
import com.erp.sri_files.dto.SendMailRequest;
import com.erp.sri_files.dto.TemplateMailRequest;
import jakarta.mail.internet.MimeMessage;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

@Service
public class MailService {

    private static final int MAX_TOTAL_ATTACHMENTS_MB = 20;

    private final JavaMailSender mailSender;
    private final TemplateEngine templateEngine;
    private final String defaultFrom;
    private final String replyTo;
    private final String displayName;

    public MailService(
            JavaMailSender mailSender,
            TemplateEngine templateEngine,
            @Value("${app.mail.from:facturacion@sri-files.local}") String defaultFrom,
            @Value("${app.mail.reply-to:facturacion@sri-files.local}") String replyTo,
            @Value("${app.mail.display-name:SRI-FILES}") String displayName
    ) {
        this.mailSender = mailSender;
        this.templateEngine = templateEngine;
        this.defaultFrom = defaultFrom;
        this.replyTo = replyTo;
        this.displayName = displayName;
    }

    @Retryable(maxAttempts = 3, backoff = @Backoff(delay = 1500))
    public java.util.UUID send(SendMailRequest req) throws Exception {
        return send(req, null);
    }

    @Retryable(maxAttempts = 3, backoff = @Backoff(delay = 1500))
    public java.util.UUID send(SendMailRequest req, String correlationId) throws Exception {
        validateAttachments(req.attachments());
        MimeMessage message = mailSender.createMimeMessage();
        boolean multipart = (req.attachments() != null && !req.attachments().isEmpty())
                || (req.inlineImages() != null && !req.inlineImages().isEmpty());
        MimeMessageHelper helper = new MimeMessageHelper(message, multipart, java.nio.charset.StandardCharsets.UTF_8.name());

        helper.setFrom(
                req.from() != null && !req.from().isBlank() ? req.from().trim() : defaultFrom,
                displayName
        );
        if (replyTo != null && !replyTo.isBlank()) {
            helper.setReplyTo(replyTo.trim());
        }
        helper.setTo(req.to().toArray(String[]::new));
        if (req.cc() != null && !req.cc().isEmpty()) {
            helper.setCc(req.cc().toArray(String[]::new));
        }
        if (req.bcc() != null && !req.bcc().isEmpty()) {
            helper.setBcc(req.bcc().toArray(String[]::new));
        }
        helper.setSubject(req.subject());
        helper.setText(req.htmlBody(), true);

        if (req.attachments() != null) {
            for (AttachmentDTO attachment : req.attachments()) {
                byte[] data = java.util.Base64.getDecoder().decode(attachment.base64());
                helper.addAttachment(
                        attachment.filename(),
                        new ByteArrayResource(data),
                        attachment.mimeType()
                );
            }
        }

        if (req.inlineImages() != null) {
            for (java.util.Map.Entry<String, String> entry : req.inlineImages().entrySet()) {
                byte[] data = java.util.Base64.getDecoder().decode(entry.getValue());
                helper.addInline(
                        entry.getKey(),
                        new ByteArrayResource(data),
                        "image/png"
                );
            }
        }

        mailSender.send(message);
        return java.util.UUID.randomUUID();
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
        try {
            mailSender.createMimeMessage();
            if (mailSender instanceof org.springframework.mail.javamail.JavaMailSenderImpl impl) {
                try (var transport = impl.getSession().getTransport()) {
                    transport.connect(
                            impl.getHost(),
                            impl.getPort(),
                            impl.getUsername(),
                            impl.getPassword()
                    );
                }
            }
            return true;
        } catch (Exception ex) {
            return false;
        }
    }
}
