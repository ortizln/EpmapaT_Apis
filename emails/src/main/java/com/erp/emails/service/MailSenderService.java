package com.erp.emails.service;

import com.erp.emails.interfaces.AttachmentStorage;
import com.erp.emails.model.EmailAccount;
import com.erp.emails.model.EmailAccountSecurityType;
import com.erp.emails.model.EmailAccountTransportType;
import com.erp.emails.model.EmailAttachment;
import com.erp.emails.model.EmailMessage;
import com.erp.emails.repository.EmailAttachmentR;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.stream.Collectors;

@Service
public class MailSenderService {

    private static final Logger log = LoggerFactory.getLogger(MailSenderService.class);

    private final EmailAttachmentR attachRepo;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final EmailBlacklistService blacklistService;
    private final AttachmentStorage attachmentStorage;

    public MailSenderService(EmailAttachmentR attachRepo, RestTemplate restTemplate, ObjectMapper objectMapper,
                             EmailBlacklistService blacklistService, AttachmentStorage attachmentStorage) {
        this.attachRepo = attachRepo;
        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper;
        this.blacklistService = blacklistService;
        this.attachmentStorage = attachmentStorage;
    }

    public void sendNow(EmailMessage msg) {
        List<EmailAttachment> attachments = attachRepo.findByEmailId(msg.getId());
        EmailAccount account = msg.getAccount();
        if (account == null) {
            throw new IllegalStateException("El mensaje no tiene una cuenta de correo asociada");
        }

        blacklistService.validateRecipients(buildRecipientList(msg));

        if (account.getTransportType() == EmailAccountTransportType.API_HTTP) {
            sendByApi(account, msg, attachments);
            return;
        }

        sendBySmtp(account, msg, attachments);
    }

    private void sendBySmtp(EmailAccount account, EmailMessage msg, List<EmailAttachment> attachments) {
        JavaMailSenderImpl mailSender = buildSender(account);
        MimeMessage mime = mailSender.createMimeMessage();
        try {
            MimeMessageHelper helper = new MimeMessageHelper(mime, true, StandardCharsets.UTF_8.name());
            if (account.getFromName() != null && !account.getFromName().isBlank()) {
                helper.setFrom(account.getFromAddress(), account.getFromName());
            } else {
                helper.setFrom(account.getFromAddress());
            }

            if (account.getReplyTo() != null && !account.getReplyTo().isBlank()) {
                helper.setReplyTo(account.getReplyTo());
            }

            helper.setTo(splitCsv(msg.getToRecipients()));

            if (msg.getCcRecipients() != null) helper.setCc(splitCsv(msg.getCcRecipients()));
            if (msg.getBccRecipients() != null) helper.setBcc(splitCsv(msg.getBccRecipients()));

            helper.setSubject(msg.getSubject());

            if (msg.getBodyHtml() != null && !msg.getBodyHtml().isBlank()) {
                helper.setText(msg.getBodyText(), msg.getBodyHtml());
            } else {
                helper.setText(msg.getBodyText() != null ? msg.getBodyText() : "");
            }

            for (EmailAttachment att : attachments) {
                helper.addAttachment(att.getFilename(), buildAttachmentResource(att), att.getContentType());
            }

            mailSender.send(mime);
        } catch (Exception e) {
            EmailAccountSecurityType effectiveSecurityType = resolveSecurityType(account);
            String context = String.format(
                    "host=%s, port=%s, protocol=%s, securityType=%s, authRequired=%s, username=%s",
                    account.getHost(),
                    account.getPort(),
                    account.getProtocol(),
                    effectiveSecurityType,
                    account.isAuthRequired(),
                    maskUsername(account.getUsername())
            );
            throw new RuntimeException("Fallo envio SMTP (" + context + "): " + e.getMessage(), e);
        }
    }

    private void sendByApi(EmailAccount account, EmailMessage msg, List<EmailAttachment> attachments) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setAccept(List.of(MediaType.APPLICATION_JSON, MediaType.ALL));

            if (account.getApiKey() != null && !account.getApiKey().isBlank()) {
                String headerName = account.getApiAuthHeader() == null || account.getApiAuthHeader().isBlank()
                        ? "Authorization"
                        : account.getApiAuthHeader();
                String headerValue = account.getApiKey();
                if (account.getApiAuthScheme() != null && !account.getApiAuthScheme().isBlank()) {
                    headerValue = account.getApiAuthScheme().trim() + " " + headerValue;
                }
                headers.set(headerName, headerValue);
            }

            Map<String, Object> payload = new LinkedHashMap<>();
            payload.put("provider", account.getProvider());
            payload.put("from", buildFrom(account));
            payload.put("replyTo", account.getReplyTo());
            payload.put("to", csvToObjects(msg.getToRecipients()));
            payload.put("cc", csvToObjects(msg.getCcRecipients()));
            payload.put("bcc", csvToObjects(msg.getBccRecipients()));
            payload.put("subject", msg.getSubject());
            payload.put("html", msg.getBodyHtml());
            payload.put("text", msg.getBodyText());
            payload.put("attachments", attachments.stream().map(this::toApiAttachment).collect(Collectors.toList()));

            HttpEntity<String> entity = new HttpEntity<>(objectMapper.writeValueAsString(payload), headers);
            ResponseEntity<String> response = restTemplate.postForEntity(account.getApiUrl(), entity, String.class);
            if (!response.getStatusCode().is2xxSuccessful()) {
                throw new RuntimeException("La API de correo respondio con estado " + response.getStatusCodeValue());
            }
        } catch (RestClientException e) {
            throw new RuntimeException("Fallo envio por API HTTP: " + e.getMessage(), e);
        } catch (IOException e) {
            throw new RuntimeException("No se pudo leer un adjunto para envio API: " + e.getMessage(), e);
        } catch (Exception e) {
            throw new RuntimeException("Fallo envio por API HTTP: " + e.getMessage(), e);
        }
    }

    private JavaMailSenderImpl buildSender(EmailAccount account) {
        JavaMailSenderImpl sender = new JavaMailSenderImpl();
        sender.setHost(account.getHost());
        sender.setPort(account.getPort());
        sender.setProtocol(account.getProtocol());
        EmailAccountSecurityType effectiveSecurityType = resolveSecurityType(account);
        boolean sslCompatibilityMode = effectiveSecurityType == EmailAccountSecurityType.SSL_TLS
                && Integer.valueOf(465).equals(account.getPort());

        if (account.getUsername() != null && !account.getUsername().isBlank()) {
            sender.setUsername(account.getUsername());
        }
        if (account.getPassword() != null && !account.getPassword().isBlank()) {
            sender.setPassword(account.getPassword());
        }

        Properties props = sender.getJavaMailProperties();
        props.put("mail.transport.protocol", account.getProtocol());
        props.put("mail.smtp.auth", Boolean.toString(account.isAuthRequired()));
        props.put("mail.smtp.starttls.enable", Boolean.toString(effectiveSecurityType == EmailAccountSecurityType.STARTTLS || sslCompatibilityMode));
        props.put("mail.smtp.starttls.required", Boolean.toString(effectiveSecurityType == EmailAccountSecurityType.STARTTLS));
        props.put("mail.smtp.ssl.enable", Boolean.toString(effectiveSecurityType == EmailAccountSecurityType.SSL_TLS));
        props.put("mail.smtp.ssl.checkserveridentity", "false");
        props.put("mail.smtp.ssl.trust", account.getHost());
        if (account.getUsername() != null && !account.getUsername().isBlank()) {
            props.put("mail.smtp.user", account.getUsername());
        }
        if (account.getPassword() != null && !account.getPassword().isBlank()) {
            props.put("mail.smtp.password", account.getPassword());
        }
        if (effectiveSecurityType == EmailAccountSecurityType.SSL_TLS) {
            props.put("mail.smtp.socketFactory.port", Integer.toString(account.getPort()));
            props.put("mail.smtp.socketFactory.class", "javax.net.ssl.SSLSocketFactory");
            props.put("mail.smtp.socketFactory.fallback", "false");
        }
        props.put("mail.smtp.connectiontimeout", "10000");
        props.put("mail.smtp.timeout", "10000");
        props.put("mail.smtp.writetimeout", "10000");
        return sender;
    }

    private EmailAccountSecurityType resolveSecurityType(EmailAccount account) {
        if (account.getSecurityType() == EmailAccountSecurityType.STARTTLS && Integer.valueOf(465).equals(account.getPort())) {
            log.warn("La cuenta SMTP {} usa puerto 465 con STARTTLS; se aplicara SSL_TLS implicito para evitar timeouts de handshake",
                    account.getCode());
            return EmailAccountSecurityType.SSL_TLS;
        }
        return account.getSecurityType();
    }

    private String maskUsername(String username) {
        if (username == null || username.isBlank()) {
            return "<empty>";
        }
        int atIndex = username.indexOf('@');
        if (atIndex > 1) {
            return username.charAt(0) + "***" + username.substring(atIndex);
        }
        return "***";
    }

    private Map<String, Object> buildFrom(EmailAccount account) {
        Map<String, Object> from = new LinkedHashMap<>();
        from.put("email", account.getFromAddress());
        if (account.getFromName() != null && !account.getFromName().isBlank()) {
            from.put("name", account.getFromName());
        }
        return from;
    }

    private List<Map<String, String>> csvToObjects(String csv) {
        if (csv == null || csv.isBlank()) {
            return List.of();
        }
        return Arrays.stream(splitCsv(csv))
                .map(String::trim)
                .filter(value -> !value.isBlank())
                .map(value -> {
                    Map<String, String> item = new LinkedHashMap<>();
                    item.put("email", value);
                    return item;
                })
                .collect(Collectors.toList());
    }

    private Map<String, Object> toApiAttachment(EmailAttachment attachment) {
        byte[] bytes = readAttachmentBytes(attachment);
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("filename", attachment.getFilename());
        item.put("contentType", attachment.getContentType());
        item.put("base64", Base64.getEncoder().encodeToString(bytes));
        return item;
    }

    private String[] splitCsv(String csv) {
        return csv.split("\\s*,\\s*");
    }

    private List<String> buildRecipientList(EmailMessage msg) {
        return java.util.stream.Stream.of(msg.getToRecipients(), msg.getCcRecipients(), msg.getBccRecipients())
                .filter(value -> value != null && !value.isBlank())
                .flatMap(value -> Arrays.stream(splitCsv(value)))
                .map(String::trim)
                .filter(value -> !value.isBlank())
                .collect(Collectors.toList());
    }

    private org.springframework.core.io.InputStreamSource buildAttachmentResource(EmailAttachment attachment) {
        byte[] bytes = readAttachmentBytes(attachment);
        return new ByteArrayResource(bytes);
    }

    private byte[] readAttachmentBytes(EmailAttachment attachment) {
        if (attachment.getContent() != null && attachment.getContent().length > 0) {
            return attachment.getContent();
        }

        File file = new File(attachment.getStorageRef());
        if (!file.exists()) {
            throw new RuntimeException("No se encontro el adjunto " + attachment.getFilename()
                    + " ni en base de datos ni en disco");
        }

        try {
            return Files.readAllBytes(file.toPath());
        } catch (IOException e) {
            throw new RuntimeException("No se pudo leer adjunto " + attachment.getFilename(), e);
        }
    }

    public void cleanupAttachments(EmailMessage msg) {
        List<EmailAttachment> attachments = attachRepo.findByEmailId(msg.getId());
        for (EmailAttachment attachment : attachments) {
            attachmentStorage.delete(attachment.getStorageRef());
        }
    }
}
