package com.erp.sri_files.retenciones.service;

import com.erp.sri_files.dto.AttachmentDTO;
import com.erp.sri_files.dto.SendMailRequest;
import com.erp.sri_files.services.MailService;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

@Service
public class RetencionEmailService {

    private final MailService mailService;

    public RetencionEmailService(MailService mailService) {
        this.mailService = mailService;
    }

    public UUID enviarRetencion(
            String correoDestino,
            String subject,
            String body,
            String baseName,
            String xmlAutorizacionCompleta,
            byte[] pdfBytes
    ) throws Exception {
        byte[] xmlBytes = xmlAutorizacionCompleta.getBytes(StandardCharsets.UTF_8);

        List<AttachmentDTO> attachments = List.of(
                new AttachmentDTO(
                        baseName + ".pdf",
                        "application/pdf",
                        java.util.Base64.getEncoder().encodeToString(pdfBytes)
                ),
                new AttachmentDTO(
                        baseName + ".xml",
                        "application/xml",
                        java.util.Base64.getEncoder().encodeToString(xmlBytes)
                )
        );

        SendMailRequest request = new SendMailRequest(
                null,
                List.of(correoDestino),
                Collections.emptyList(),
                Collections.emptyList(),
                subject,
                body.replace("\n", "<br>"),
                attachments,
                Collections.emptyMap()
        );

        return mailService.send(request, baseName);
    }
}
