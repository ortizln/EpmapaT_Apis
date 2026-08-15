package com.erp.sri_files.mail;

import com.erp.sri_files.dto.SendMailRequest;
import com.erp.sri_files.services.MailService;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
public class EmailMicroserviceClient implements EmailClient {

    private final MailService mailService;

    public EmailMicroserviceClient(MailService mailService) {
        this.mailService = mailService;
    }

    @Override
    public void enviar(String destinatario, String asunto, String cuerpo) {
        enviar(null, destinatario, asunto, cuerpo);
    }

    @Override
    public void enviar(String from, String destinatario, String asunto, String cuerpo) {
        SendMailRequest request = new SendMailRequest(
                from,
                List.of(destinatario),
                List.of(),
                List.of(),
                asunto,
                cuerpo,
                List.of(),
                Map.of()
        );
        try {
            mailService.send(request);
        } catch (Exception ex) {
            throw new IllegalStateException("No fue posible enviar el correo del documento", ex);
        }
    }
}
