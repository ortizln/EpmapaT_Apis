package com.erp.emails.repository;

import com.erp.emails.model.EmailAttachment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface EmailAttachmentR extends JpaRepository<EmailAttachment, UUID> {
    List<EmailAttachment> findByEmailId(UUID emailId);

}
