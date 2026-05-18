package com.erp.emails.dtos;

import com.erp.emails.model.EmailStatus;
import com.erp.emails.model.EmailType;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public class EmailResponse {
    public UUID id;
    public EmailType type;
    public EmailStatus status;
    public String subject;
    public String correlationId;
    public Long accountId;
    public String accountCode;
    public String accountName;
    public String fromAddress;
    public int attempts;
    public String lastError;
    public OffsetDateTime createdAt;
    public OffsetDateTime sentAt;
    public List<String> to;
    public List<String> cc;
    public List<String> bcc;
    public String bodyHtml;
    public String bodyText;
    public List<EmailAttachmentResponse> attachments;

    public static class EmailAttachmentResponse {
        public String name;
        public String contentType;
        public long size;
    }
}
