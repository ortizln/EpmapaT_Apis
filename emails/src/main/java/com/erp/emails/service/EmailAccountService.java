package com.erp.emails.service;

import com.erp.emails.dtos.EmailAccountRequest;
import com.erp.emails.dtos.EmailAccountResponse;
import com.erp.emails.model.EmailAccount;
import com.erp.emails.model.EmailAccountSecurityType;
import com.erp.emails.model.EmailAccountTransportType;
import com.erp.emails.model.EmailType;
import com.erp.emails.repository.EmailAccountR;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class EmailAccountService {

    private final EmailAccountR accountRepo;

    public EmailAccountService(EmailAccountR accountRepo) {
        this.accountRepo = accountRepo;
    }

    @Transactional(readOnly = true)
    public List<EmailAccountResponse> list(Boolean active) {
        List<EmailAccount> accounts = active == null ? accountRepo.findAll() : accountRepo.findByActive(active);
        return accounts.stream().map(account -> toResponse(account, false)).toList();
    }

    @Transactional(readOnly = true)
    public EmailAccountResponse get(Long id) {
        return toResponse(getEntity(id), true);
    }

    @Transactional
    public EmailAccountResponse create(EmailAccountRequest req) {
        String code = normalizeCode(req.code);
        if (accountRepo.existsByCodeIgnoreCase(code)) {
            throw new IllegalArgumentException("Ya existe una cuenta de correo con el codigo " + code);
        }

        EmailAccount account = new EmailAccount();
        apply(account, req, true);
        account.setCode(code);
        account = accountRepo.save(account);
        ensureSingleDefaults(account);
        return toResponse(account, false);
    }

    @Transactional
    public EmailAccountResponse update(Long id, EmailAccountRequest req) {
        EmailAccount account = getEntity(id);
        String code = normalizeCode(req.code);
        if (accountRepo.existsByCodeIgnoreCaseAndIdNot(code, id)) {
            throw new IllegalArgumentException("Ya existe una cuenta de correo con el codigo " + code);
        }

        apply(account, req, false);
        account.setCode(code);
        account = accountRepo.save(account);
        ensureSingleDefaults(account);
        return toResponse(account, false);
    }

    @Transactional
    public EmailAccountResponse activate(Long id, boolean active) {
        EmailAccount account = getEntity(id);
        account.setActive(active);
        account = accountRepo.save(account);
        return toResponse(account, false);
    }

    @Transactional(readOnly = true)
    public EmailAccount resolveAccount(Long requestedAccountId, EmailType emailType) {
        if (requestedAccountId != null) {
            EmailAccount account = getEntity(requestedAccountId);
            if (!account.isActive()) {
                throw new IllegalArgumentException("La cuenta de correo seleccionada esta inactiva");
            }
            return account;
        }

        if (emailType != null) {
            var byType = accountRepo.findFirstByActiveTrueAndDefaultForType(emailType);
            if (byType.isPresent()) {
                return byType.get();
            }
        }

        return accountRepo.findFirstByActiveTrueAndDefaultAccountTrue()
                .orElseThrow(() -> new IllegalStateException("No existe una cuenta de correo activa configurada para el tipo solicitado"));
    }

    @Transactional(readOnly = true)
    public EmailAccount getEntity(Long id) {
        return accountRepo.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("No existe la cuenta de correo con id " + id));
    }

    private void apply(EmailAccount account, EmailAccountRequest req, boolean creating) {
        EmailAccountTransportType transportType = resolveTransportType(req, account);
        validate(req, creating, account.getPassword(), account.getApiKey(), transportType);

        account.setName(req.name.trim());
        account.setProvider(trimToNull(req.provider));
        account.setFromAddress(req.fromAddress.trim());
        account.setFromName(trimToNull(req.fromName));
        String replyTo = trimToNull(req.replyTo);
        account.setReplyTo(replyTo == null ? account.getFromAddress() : replyTo);
        account.setTransportType(transportType);
        account.setHost(trimToNull(req.host));
        account.setPort(req.port);
        account.setProtocol(trimToNull(req.protocol) == null ? "smtp" : req.protocol.trim().toLowerCase());
        account.setSecurityType(resolveSecurityType(req, account, transportType));
        account.setAuthRequired(req.authRequired);
        account.setUsername(trimToNull(req.username));
        account.setApiUrl(trimToNull(req.apiUrl));
        account.setApiAuthHeader(trimToNull(req.apiAuthHeader));
        account.setApiAuthScheme(trimToNull(req.apiAuthScheme));
        account.setActive(req.active);
        account.setDefaultAccount(req.defaultAccount);
        account.setDefaultForType(req.defaultForType);

        String password = trimToNull(req.password);
        if (password != null) {
            account.setPassword(password);
        } else if (creating) {
            account.setPassword(null);
        }

        String apiKey = trimToNull(req.apiKey);
        if (apiKey != null) {
            account.setApiKey(apiKey);
        } else if (creating) {
            account.setApiKey(null);
        }
    }

    private void validate(EmailAccountRequest req, boolean creating, String existingPassword, String existingApiKey,
                          EmailAccountTransportType transportType) {
        if (transportType == EmailAccountTransportType.SMTP) {
            if (trimToNull(req.host) == null) throw new IllegalArgumentException("La cuenta SMTP requiere host");
            if (req.port == null) throw new IllegalArgumentException("La cuenta SMTP requiere port");
            if (req.authRequired && trimToNull(req.username) == null) {
                throw new IllegalArgumentException("La cuenta SMTP requiere username cuando authRequired=true");
            }
            if (req.authRequired && creating && trimToNull(req.password) == null) {
                throw new IllegalArgumentException("La cuenta SMTP requiere password cuando authRequired=true");
            }
            if (req.authRequired && !creating && trimToNull(req.password) == null && trimToNull(existingPassword) == null) {
                throw new IllegalArgumentException("La cuenta SMTP requiere password cuando authRequired=true");
            }
            return;
        }

        if (transportType == EmailAccountTransportType.API_HTTP) {
            if (trimToNull(req.apiUrl) == null) throw new IllegalArgumentException("La cuenta API_HTTP requiere apiUrl");
            if (creating && trimToNull(req.apiKey) == null) throw new IllegalArgumentException("La cuenta API_HTTP requiere apiKey");
            if (!creating && trimToNull(req.apiKey) == null && trimToNull(existingApiKey) == null) {
                throw new IllegalArgumentException("La cuenta API_HTTP requiere apiKey");
            }
        }
    }

    private EmailAccountTransportType resolveTransportType(EmailAccountRequest req, EmailAccount account) {
        if (req.transportType != null) return req.transportType;
        if (account.getTransportType() != null) return account.getTransportType();
        return EmailAccountTransportType.SMTP;
    }

    private EmailAccountSecurityType resolveSecurityType(EmailAccountRequest req, EmailAccount account, EmailAccountTransportType transportType) {
        if (transportType != EmailAccountTransportType.SMTP) return req.securityType;
        if (req.securityType != null) return req.securityType;
        if (account.getSecurityType() != null) return account.getSecurityType();
        return EmailAccountSecurityType.STARTTLS;
    }

    private void ensureSingleDefaults(EmailAccount saved) {
        if (saved.isDefaultAccount()) {
            accountRepo.findAll().stream()
                    .filter(other -> !other.getId().equals(saved.getId()) && other.isDefaultAccount())
                    .forEach(other -> {
                        other.setDefaultAccount(false);
                        accountRepo.save(other);
                    });
        }

        if (saved.getDefaultForType() != null) {
            accountRepo.findAll().stream()
                    .filter(other -> !other.getId().equals(saved.getId()))
                    .filter(other -> saved.getDefaultForType().equals(other.getDefaultForType()))
                    .forEach(other -> {
                        other.setDefaultForType(null);
                        accountRepo.save(other);
                    });
        }
    }

    private EmailAccountResponse toResponse(EmailAccount account, boolean includeSecrets) {
        EmailAccountResponse r = new EmailAccountResponse();
        r.id = account.getId();
        r.code = account.getCode();
        r.name = account.getName();
        r.provider = account.getProvider();
        r.fromAddress = account.getFromAddress();
        r.fromName = account.getFromName();
        r.replyTo = account.getReplyTo();
        r.transportType = account.getTransportType();
        r.host = account.getHost();
        r.port = account.getPort();
        r.protocol = account.getProtocol();
        r.securityType = account.getSecurityType();
        r.authRequired = account.isAuthRequired();
        r.username = account.getUsername();
        r.password = includeSecrets ? account.getPassword() : null;
        r.hasPassword = account.getPassword() != null && !account.getPassword().isBlank();
        r.apiUrl = account.getApiUrl();
        r.apiAuthHeader = account.getApiAuthHeader();
        r.apiAuthScheme = account.getApiAuthScheme();
        r.hasApiKey = account.getApiKey() != null && !account.getApiKey().isBlank();
        r.active = account.isActive();
        r.defaultAccount = account.isDefaultAccount();
        r.defaultForType = account.getDefaultForType();
        r.createdAt = account.getCreatedAt();
        r.updatedAt = account.getUpdatedAt();
        return r;
    }

    private String normalizeCode(String code) {
        return code == null ? null : code.trim().toUpperCase().replace(' ', '_');
    }

    private String trimToNull(String value) {
        if (value == null) return null;
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
