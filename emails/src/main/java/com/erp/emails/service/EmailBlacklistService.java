package com.erp.emails.service;

import com.erp.emails.dtos.EmailBlacklistRequest;
import com.erp.emails.dtos.EmailBlacklistResponse;
import com.erp.emails.model.EmailBlacklist;
import com.erp.emails.repository.EmailBlacklistR;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@Service
public class EmailBlacklistService {

    private final EmailBlacklistR blacklistRepo;

    public EmailBlacklistService(EmailBlacklistR blacklistRepo) {
        this.blacklistRepo = blacklistRepo;
    }

    @Transactional(readOnly = true)
    public List<EmailBlacklistResponse> list(Boolean active) {
        List<EmailBlacklist> items = active == null ? blacklistRepo.findAll() : blacklistRepo.findByActive(active);
        return items.stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public EmailBlacklistResponse get(Long id) {
        return toResponse(getEntity(id));
    }

    @Transactional
    public EmailBlacklistResponse create(EmailBlacklistRequest req) {
        String value = normalizeValue(req.value);
        if (blacklistRepo.existsByTypeAndValueIgnoreCase(req.type, value)) {
            throw new IllegalArgumentException("Ya existe una regla de lista negra para ese valor");
        }
        EmailBlacklist item = new EmailBlacklist();
        apply(item, req, value);
        return toResponse(blacklistRepo.save(item));
    }

    @Transactional
    public EmailBlacklistResponse update(Long id, EmailBlacklistRequest req) {
        String value = normalizeValue(req.value);
        if (blacklistRepo.existsByTypeAndValueIgnoreCaseAndIdNot(req.type, value, id)) {
            throw new IllegalArgumentException("Ya existe una regla de lista negra para ese valor");
        }
        EmailBlacklist item = getEntity(id);
        apply(item, req, value);
        return toResponse(blacklistRepo.save(item));
    }

    @Transactional
    public EmailBlacklistResponse activate(Long id, boolean active) {
        EmailBlacklist item = getEntity(id);
        item.setActive(active);
        return toResponse(blacklistRepo.save(item));
    }

    @Transactional(readOnly = true)
    public void validateRecipients(List<String> recipients) {
        if (recipients == null || recipients.isEmpty()) return;

        List<EmailBlacklist> rules = blacklistRepo.findByActive(true);
        List<String> violations = new ArrayList<>();

        for (String raw : recipients) {
            String email = normalizeEmail(raw);
            if (email == null) continue;
            String host = extractHost(email);
            for (EmailBlacklist rule : rules) {
                if (matches(rule, email, host)) {
                    violations.add(email + " bloqueado por " + rule.getType() + "=" + rule.getValue());
                    break;
                }
            }
        }

        if (!violations.isEmpty()) {
            throw new EmailBlacklistViolationException("Destinatarios bloqueados por lista negra: " + String.join("; ", violations));
        }
    }

    @Transactional(readOnly = true)
    public EmailBlacklist getEntity(Long id) {
        return blacklistRepo.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("No existe la regla de lista negra con id " + id));
    }

    private boolean matches(EmailBlacklist rule, String email, String host) {
        String value = normalizeValue(rule.getValue());
        return switch (rule.getType()) {
            case EMAIL -> email.equalsIgnoreCase(value);
            case HOST, DOMAIN -> host != null && host.equalsIgnoreCase(value);
        };
    }

    private void apply(EmailBlacklist item, EmailBlacklistRequest req, String normalizedValue) {
        item.setType(req.type);
        item.setValue(normalizedValue);
        item.setReason(trimToNull(req.reason));
        item.setActive(req.active);
    }

    private EmailBlacklistResponse toResponse(EmailBlacklist item) {
        EmailBlacklistResponse r = new EmailBlacklistResponse();
        r.id = item.getId();
        r.type = item.getType();
        r.value = item.getValue();
        r.reason = item.getReason();
        r.active = item.isActive();
        r.createdAt = item.getCreatedAt();
        r.updatedAt = item.getUpdatedAt();
        return r;
    }

    private String normalizeValue(String value) {
        if (value == null) return null;
        return value.trim().toLowerCase(Locale.ROOT);
    }

    private String normalizeEmail(String email) {
        String value = trimToNull(email);
        if (value == null || !value.contains("@")) return null;
        return value.toLowerCase(Locale.ROOT);
    }

    private String extractHost(String email) {
        int at = email.indexOf('@');
        if (at < 0 || at == email.length() - 1) return null;
        return email.substring(at + 1).toLowerCase(Locale.ROOT);
    }

    private String trimToNull(String value) {
        if (value == null) return null;
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
