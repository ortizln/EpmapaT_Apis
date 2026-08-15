package com.erp.sri_files.services;

import com.erp.sri_files.dto.AttachmentDTO;
import com.erp.sri_files.dto.EmailMsAttachmentInput;
import com.erp.sri_files.dto.EmailMsIdResponse;
import com.erp.sri_files.dto.EmailMsSendEmailRequest;
import com.erp.sri_files.dto.SendMailRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class EmailMsClientService {

    private final RestTemplate restTemplate;
    private final String baseUrl;

    public EmailMsClientService(
            RestTemplate restTemplate,
            @Value("${app.email-ms.base-url:http://msvc-emails:9099}") String baseUrl
    ) {
        this.restTemplate = restTemplate;
        this.baseUrl = baseUrl;
    }

    public UUID enqueueDocumentEmail(SendMailRequest req, String correlationId) {
        EmailMsSendEmailRequest payload = new EmailMsSendEmailRequest(
                req.to(),
                req.cc(),
                req.bcc(),
                req.subject(),
                null,
                null,
                req.htmlBody(),
                stripHtml(req.htmlBody()),
                correlationId,
                mapAttachments(req.attachments())
        );

        try {
            ResponseEntity<EmailMsIdResponse> response = restTemplate.postForEntity(
                    baseUrl + "/api/v1/emails/documents",
                    payload,
                    EmailMsIdResponse.class
            );

            if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null || response.getBody().id() == null) {
                throw new IllegalStateException("msvc-emails no devolvio un identificador de encolamiento");
            }

            return response.getBody().id();
        } catch (RestClientException ex) {
            throw new IllegalStateException("No fue posible conectar con msvc-emails: " + ex.getMessage(), ex);
        }
    }

    public boolean health() {
        try {
            ResponseEntity<Map> response = restTemplate.getForEntity(baseUrl + "/actuator/health", Map.class);
            return response.getStatusCode().is2xxSuccessful();
        } catch (RestClientException ex) {
            return false;
        }
    }

    private List<EmailMsAttachmentInput> mapAttachments(List<AttachmentDTO> attachments) {
        if (attachments == null || attachments.isEmpty()) {
            return List.of();
        }

        return attachments.stream()
                .map(att -> new EmailMsAttachmentInput(att.filename(), att.mimeType(), att.base64()))
                .toList();
    }

    private String stripHtml(String html) {
        if (html == null || html.isBlank()) {
            return "";
        }

        return html.replaceAll("<[^>]*>", " ")
                .replace("&nbsp;", " ")
                .replaceAll("\\s+", " ")
                .trim();
    }
}
