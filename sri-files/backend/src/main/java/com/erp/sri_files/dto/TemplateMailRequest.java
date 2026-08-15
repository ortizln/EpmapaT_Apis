package com.erp.sri_files.dto;

public record TemplateMailRequest(
        String from,
        java.util.List<String> to,
        java.util.List<String> cc,
        java.util.List<String> bcc,
        String subject,
        String template,                       // nombre plantilla Thymeleaf (sin .html)
        java.util.Map<String, Object> model,   // variables para plantilla
        java.util.List<AttachmentDTO> attachments
) {}