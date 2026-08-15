package com.erp.sri_files.mail;

public interface EmailClient {

    void enviar(String destinatario, String asunto, String cuerpo);

    void enviar(String from, String destinatario, String asunto, String cuerpo);
}
