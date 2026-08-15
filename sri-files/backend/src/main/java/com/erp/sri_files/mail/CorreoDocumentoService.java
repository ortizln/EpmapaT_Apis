package com.erp.sri_files.mail;

import com.erp.sri_files.domain.documento.Empresa;
import org.springframework.stereotype.Service;

@Service
public class CorreoDocumentoService {

    private final EmailClient emailClient;

    public CorreoDocumentoService(EmailClient emailClient) {
        this.emailClient = emailClient;
    }

    public void enviarNotificacionBasica(String destinatario, String asunto, String cuerpo) {
        emailClient.enviar(destinatario, asunto, cuerpo);
    }

    public void enviarNotificacionBasica(Empresa empresa, String destinatario, String asunto, String cuerpo) {
        String remitente = empresa == null ? null : empresa.getCorreoNotificaciones();
        emailClient.enviar(remitente, destinatario, asunto, cuerpo);
    }
}
