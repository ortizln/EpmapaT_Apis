package com.erp.emails.controller;

import com.erp.emails.component.EmailSpecs;
import com.erp.emails.dtos.EmailResponse;
import com.erp.emails.dtos.SendEmailRequest;
import com.erp.emails.model.EmailAttachment;
import com.erp.emails.model.EmailMessage;
import com.erp.emails.model.EmailStatus;
import com.erp.emails.model.EmailType;
import com.erp.emails.repository.EmailAccountR;
import com.erp.emails.repository.EmailAttachmentR;
import com.erp.emails.repository.EmailBlacklistR;
import com.erp.emails.repository.EmailMessageR;
import com.erp.emails.service.EmailComposerService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/emails")
public class EmailController {

    private final EmailComposerService composer;
    private final EmailMessageR emailRepo;
    private final EmailAttachmentR attachmentRepo;
    private final EmailAccountR accountRepo;
    private final EmailBlacklistR blacklistRepo;

    public EmailController(
            EmailComposerService composer,
            EmailMessageR emailRepo,
            EmailAttachmentR attachmentRepo,
            EmailAccountR accountRepo,
            EmailBlacklistR blacklistRepo
    ) {
        this.composer = composer;
        this.emailRepo = emailRepo;
        this.attachmentRepo = attachmentRepo;
        this.accountRepo = accountRepo;
        this.blacklistRepo = blacklistRepo;
    }

    @PostMapping("/documents")
    public ResponseEntity<?> sendDocuments(@Valid @RequestBody SendEmailRequest req) {
        return enqueue(EmailType.DOC_ELECTRONICO, req);
    }

    @PostMapping("/notifications")
    public ResponseEntity<?> sendNotifications(@Valid @RequestBody SendEmailRequest req) {
        return enqueue(EmailType.NOTIFICACION, req);
    }

    @PostMapping("/custom")
    public ResponseEntity<?> sendCustom(@Valid @RequestBody SendEmailRequest req) {
        return enqueue(EmailType.CUSTOM, req);
    }

    @GetMapping("/{id}")
    public ResponseEntity<EmailResponse> getOne(@PathVariable UUID id) {
        return emailRepo.findById(id)
                .map(e -> ResponseEntity.ok(toResponse(e)))
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping
    public Page<EmailResponse> list(
            @RequestParam(required = false) EmailStatus status,
            @RequestParam(required = false) EmailType type,
            @RequestParam(required = false) String correlationId,
            @RequestParam(required = false) Long accountId,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String dateFrom,
            @RequestParam(required = false) String dateTo,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        var pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        var spec = EmailSpecs.filter(
                status,
                type,
                correlationId,
                accountId,
                search,
                parseDate(dateFrom),
                parseDate(dateTo)
        );
        return emailRepo.findAll(spec, pageable).map(this::toResponse);
    }

    @GetMapping("/summary")
    public ResponseEntity<EmailSummaryResponse> summary() {
        long pending = emailRepo.count(EmailSpecs.filter(EmailStatus.PENDING, null, null, null, null, null, null));
        long failed = emailRepo.count(EmailSpecs.filter(EmailStatus.FAILED, null, null, null, null, null, null));
        long activeAccounts = accountRepo.findByActive(true).size();
        long blockedDomains = blacklistRepo.findByActive(true).size();
        return ResponseEntity.ok(new EmailSummaryResponse(activeAccounts, pending, failed, blockedDomains));
    }

    @GetMapping("/pending/stale")
    public Page<EmailResponse> listStalePending(
            @RequestParam(defaultValue = "30") int olderThanMinutes,
            @RequestParam(required = false) Integer minAttempts,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        OffsetDateTime createdBefore = OffsetDateTime.now().minusMinutes(Math.max(olderThanMinutes, 0));
        var pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.ASC, "createdAt"));
        var spec = EmailSpecs.pendingStale(createdBefore, minAttempts);
        return emailRepo.findAll(spec, pageable).map(this::toResponse);
    }

    @PostMapping("/{id}/retry")
    public ResponseEntity<?> retry(@PathVariable UUID id) {
        return emailRepo.findById(id).map(e -> {
            e.setAttempts(0);
            e.setStatus(EmailStatus.PENDING);
            e.setLastError(null);
            e.setSentAt(null);
            emailRepo.save(e);
            return ResponseEntity.accepted().body(new IdResponse(id));
        }).orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/{id}/cancel")
    public ResponseEntity<?> cancel(@PathVariable UUID id) {
        return emailRepo.findById(id).map(e -> {
            if (e.getStatus() == EmailStatus.PENDING) {
                e.setStatus(EmailStatus.CANCELLED);
                emailRepo.save(e);
                return ResponseEntity.ok().build();
            }
            return ResponseEntity.status(409).body("Solo se puede cancelar si esta PENDING");
        }).orElse(ResponseEntity.notFound().build());
    }

    private EmailResponse toResponse(EmailMessage e) {
        EmailResponse r = new EmailResponse();
        r.id = e.getId();
        r.type = e.getType();
        r.status = e.getStatus();
        r.subject = e.getSubject();
        r.correlationId = e.getCorrelationId();
        if (e.getAccount() != null) {
            r.accountId = e.getAccount().getId();
            r.accountCode = e.getAccount().getCode();
            r.accountName = e.getAccount().getName();
        }
        r.fromAddress = e.getFromAddress();
        r.attempts = e.getAttempts();
        r.lastError = e.getLastError();
        r.createdAt = e.getCreatedAt();
        r.sentAt = e.getSentAt();
        r.to = splitCsv(e.getToRecipients());
        r.cc = splitCsv(e.getCcRecipients());
        r.bcc = splitCsv(e.getBccRecipients());
        r.bodyHtml = e.getBodyHtml();
        r.bodyText = e.getBodyText();
        r.attachments = attachmentRepo.findByEmailId(e.getId()).stream().map(this::toAttachmentResponse).toList();
        return r;
    }

    private EmailResponse.EmailAttachmentResponse toAttachmentResponse(EmailAttachment attachment) {
        EmailResponse.EmailAttachmentResponse response = new EmailResponse.EmailAttachmentResponse();
        response.name = attachment.getFilename();
        response.contentType = attachment.getContentType();
        response.size = attachment.getSize();
        return response;
    }

    private List<String> splitCsv(String value) {
        if (value == null || value.isBlank()) {
            return List.of();
        }
        return Arrays.stream(value.split("\\s*,\\s*"))
                .map(String::trim)
                .filter(item -> !item.isBlank())
                .toList();
    }

    public record IdResponse(UUID id) {}
    public record EmailSummaryResponse(long activeAccounts, long pendingEmails, long failedEmails, long blockedDomains) {}

    private ResponseEntity<?> enqueue(EmailType type, SendEmailRequest req) {
        try {
            UUID id = composer.enqueue(type, req);
            return ResponseEntity.accepted().body(new IdResponse(id));
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(ex.getMessage());
        } catch (IllegalStateException ex) {
            return ResponseEntity.status(409).body(ex.getMessage());
        }
    }

    private LocalDate parseDate(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return LocalDate.parse(value);
    }
}
