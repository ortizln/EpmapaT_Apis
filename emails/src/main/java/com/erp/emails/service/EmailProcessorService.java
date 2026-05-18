package com.erp.emails.service;

import com.erp.emails.model.EmailMessage;
import com.erp.emails.model.EmailStatus;
import com.erp.emails.repository.EmailMessageR;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.OffsetDateTime;
import java.util.UUID;

@Service
public class EmailProcessorService {

    private static final Logger log = LoggerFactory.getLogger(EmailProcessorService.class);

    private final MailSenderService sender;
    private final EmailMessageR emailRepo;
    private final TransactionTemplate requiresNewTx;

    public EmailProcessorService(MailSenderService sender, EmailMessageR emailRepo, PlatformTransactionManager txManager) {
        this.sender = sender;
        this.emailRepo = emailRepo;
        this.requiresNewTx = new TransactionTemplate(txManager);
        this.requiresNewTx.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
    }

    public void procesarEmail(UUID emailId, int maxAttempts) {
        EmailMessage msg = claimForProcessing(emailId, maxAttempts);
        if (msg == null) {
            return;
        }

        try {
            sender.sendNow(msg);
            markSent(msg.getId());
            cleanupIfTerminal(msg);
        } catch (EmailBlacklistViolationException e) {
            markFailure(msg.getId(), e.getMessage(), true);
            cleanupIfTerminal(msg);
        } catch (Exception e) {
            boolean terminalFailure = msg.getAttempts() >= maxAttempts || isMissingAttachmentFailure(e);
            markFailure(msg.getId(), e.getMessage(), terminalFailure);
            if (terminalFailure) {
                cleanupIfTerminal(msg);
            }
            log.error("Fallo procesando email {}: {}", msg.getId(), e.getMessage(), e);
        }
    }

    private boolean isMissingAttachmentFailure(Throwable error) {
        Throwable current = error;
        while (current != null) {
            String message = current.getMessage();
            if (current instanceof java.io.FileNotFoundException) {
                return true;
            }
            if (message != null) {
                String normalized = message.toLowerCase();
                if (normalized.contains("no se encontro el adjunto")
                        || normalized.contains("no se encontró el adjunto")
                        || normalized.contains("email_attachment")
                        || normalized.contains("file not found")) {
                    return true;
                }
            }
            current = current.getCause();
        }
        return false;
    }

    private EmailMessage claimForProcessing(UUID emailId, int maxAttempts) {
        return requiresNewTx.execute(status -> {
            EmailMessage msg = emailRepo.lockById(emailId).orElse(null);
            if (msg == null) {
                return null;
            }
            if (msg.getStatus() != EmailStatus.PENDING || msg.getAttempts() >= maxAttempts) {
                return null;
            }
            msg.setAttempts(msg.getAttempts() + 1);
            return emailRepo.saveAndFlush(msg);
        });
    }

    private void markSent(UUID emailId) {
        requiresNewTx.executeWithoutResult(status -> {
            EmailMessage msg = emailRepo.lockById(emailId).orElse(null);
            if (msg == null) {
                return;
            }

            msg.setStatus(EmailStatus.SENT);
            msg.setSentAt(OffsetDateTime.now());
            msg.setLastError(null);
            emailRepo.saveAndFlush(msg);
        });
    }

    private void markFailure(UUID emailId, String errorMessage, boolean terminalFailure) {
        requiresNewTx.executeWithoutResult(status -> {
            EmailMessage msg = emailRepo.lockById(emailId).orElse(null);
            if (msg == null) {
                return;
            }

            msg.setLastError(truncate(errorMessage, 1900));
            if (terminalFailure) {
                msg.setStatus(EmailStatus.FAILED);
            }
            emailRepo.saveAndFlush(msg);
        });
    }

    private void cleanupIfTerminal(EmailMessage msg) {
        try {
            sender.cleanupAttachments(msg);
        } catch (Exception e) {
            log.warn("No se pudieron limpiar adjuntos del email {}: {}", msg.getId(), e.getMessage());
        }
    }

    private String truncate(String value, int maxLength) {
        if (value == null || value.length() <= maxLength) {
            return value;
        }
        return value.substring(0, maxLength);
    }
}
