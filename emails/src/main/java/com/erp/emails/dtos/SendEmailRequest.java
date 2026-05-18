package com.erp.emails.dtos;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import java.util.List;
import java.util.Map;

public class SendEmailRequest {
    @NotEmpty public List<String> to;
    public List<String> cc;
    public List<String> bcc;

    @NotBlank public String subject;

    // Para plantillas
    public String templateKey;
    public Map<String, Object> variables;

    // Para custom
    public String html;
    public String text;

    public String correlationId;
    public Long accountId;
    public List<AttachmentInput> attachments;
}
