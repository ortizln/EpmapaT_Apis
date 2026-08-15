package com.erp.sri_files.dto;

import java.util.List;
import java.util.Map;

public record EmailMsSendEmailRequest(
        List<String> to,
        List<String> cc,
        List<String> bcc,
        String subject,
        String templateKey,
        Map<String, Object> variables,
        String html,
        String text,
        String correlationId,
        List<EmailMsAttachmentInput> attachments
) {}
