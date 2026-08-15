package com.erp.sri_files.dto;

public record SendMailRequest(
        @jakarta.validation.constraints.Email String from,            // opcional: si null usamos app.mail.from
        @jakarta.validation.constraints.NotEmpty java.util.List<@jakarta.validation.constraints.Email String> to,
        java.util.List<@jakarta.validation.constraints.Email String> cc,
        java.util.List<@jakarta.validation.constraints.Email String> bcc,
        @jakarta.validation.constraints.NotBlank String subject,
        @jakarta.validation.constraints.NotBlank String htmlBody,
        java.util.List<AttachmentDTO> attachments,                   // adjuntos
        java.util.Map<String, String> inlineImages                   // nombre -> base64 PNG/JPG (cid)
) {}