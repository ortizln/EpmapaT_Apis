package com.erp.emails.service;

import com.erp.emails.dtos.AttachmentInput;
import com.erp.emails.dtos.SendEmailRequest;
import com.erp.emails.interfaces.AttachmentStorage;
import com.erp.emails.model.EmailAttachment;
import com.erp.emails.model.EmailAccount;
import com.erp.emails.model.EmailMessage;
import com.erp.emails.model.EmailStatus;
import com.erp.emails.model.EmailType;
import com.erp.emails.repository.EmailAttachmentR;
import com.erp.emails.repository.EmailMessageR;
import jakarta.transaction.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Base64;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class EmailComposerService {

    private static final Logger log = LoggerFactory.getLogger(EmailComposerService.class);

    private final EmailMessageR emailRepo;
    private final EmailAttachmentR attachRepo;
    private final TemplateService templateService;
    private final AttachmentStorage storage;
    private final EmailAccountService accountService;
    private final EmailBlacklistService blacklistService;

    public EmailComposerService(EmailMessageR emailRepo,
                                EmailAttachmentR attachRepo,
                                TemplateService templateService,
                                AttachmentStorage storage,
                                EmailAccountService accountService,
                                EmailBlacklistService blacklistService) {
        this.emailRepo = emailRepo;
        this.attachRepo = attachRepo;
        this.templateService = templateService;
        this.storage = storage;
        this.accountService = accountService;
        this.blacklistService = blacklistService;
    }

    @Transactional
    public UUID enqueue(EmailType type, SendEmailRequest req) {
        blacklistService.validateRecipients(mergeRecipients(req.to, req.cc, req.bcc));
        EmailAccount account = accountService.resolveAccount(req.accountId, type);

        EmailMessage msg = new EmailMessage();
        msg.setType(type);
        msg.setStatus(EmailStatus.PENDING);
        msg.setToRecipients(join(req.to));
        msg.setCcRecipients(join(req.cc));
        msg.setBccRecipients(join(req.bcc));
        msg.setSubject(req.subject);
        msg.setCorrelationId(req.correlationId);
        msg.setAccount(account);
        msg.setFromAddress(account.getFromAddress());
        msg.setAttempts(0);

        // cuerpo
        String html = null;
        String text = req.text;

        if (req.templateKey != null && !req.templateKey.isBlank()) {
            html = templateService.render(req.templateKey, req.variables);
        } else if (req.html != null && !req.html.isBlank()) {
            html = req.html;
        }
        msg.setBodyHtml(html);
        msg.setBodyText(text);

        EmailMessage saved = emailRepo.save(msg);

        // adjuntos
        if (req.attachments != null) {
            for (AttachmentInput a : req.attachments) {
                byte[] bytes = Base64.getDecoder().decode(a.base64);
                String storageRef = buildStorageRef(a.name);
                long size = bytes.length;
                String sha256 = null;

                try {
                    var stored = storage.save(a.name, a.contentType, bytes);
                    storageRef = stored.storageRef();
                    size = stored.size();
                    sha256 = stored.sha256();
                } catch (RuntimeException ex) {
                    log.warn("No se pudo guardar adjunto en disco para {}, se usara solo respaldo en BD: {}",
                            a.name, ex.getMessage());
                }

                EmailAttachment att = new EmailAttachment();
                att.setEmail(saved);
                att.setFilename(a.name);
                att.setContentType(a.contentType);
                att.setSize(size);
                att.setStorageRef(storageRef);
                att.setContent(bytes);
                att.setSha256(sha256);
                attachRepo.save(att);
            }
        }
        return saved.getId();
    }

    private String join(List<String> xs) {
        if (xs == null || xs.isEmpty()) return null;
        return xs.stream().map(String::trim).filter(s -> !s.isBlank()).collect(Collectors.joining(","));
    }

    private String buildStorageRef(String filename) {
        String safeName = filename == null ? "attachment" : filename.replaceAll("[^a-zA-Z0-9._-]", "_");
        return "db://" + UUID.randomUUID() + "_" + safeName;
    }

    @SafeVarargs
    private final List<String> mergeRecipients(List<String>... groups) {
        return java.util.Arrays.stream(groups)
                .filter(list -> list != null && !list.isEmpty())
                .flatMap(List::stream)
                .collect(Collectors.toList());
    }
}
