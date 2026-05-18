package com.erp.emails.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
public class EmailAttachmentSchemaInitializer {

    private static final Logger log = LoggerFactory.getLogger(EmailAttachmentSchemaInitializer.class);

    private final JdbcTemplate jdbcTemplate;

    public EmailAttachmentSchemaInitializer(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void ensureAttachmentContentColumn() {
        try {
            jdbcTemplate.execute("ALTER TABLE public.email_attachment ADD COLUMN IF NOT EXISTS content bytea NULL");
            log.info("Verificado esquema de email_attachment: columna content disponible");
        } catch (Exception ex) {
            log.warn("No se pudo verificar/agregar la columna content en email_attachment: {}", ex.getMessage());
        }
    }
}
