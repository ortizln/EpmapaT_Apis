package com.erp.emails.repository;

import com.erp.emails.model.EmailBlacklist;
import com.erp.emails.model.EmailBlacklistType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface EmailBlacklistR extends JpaRepository<EmailBlacklist, Long> {
    List<EmailBlacklist> findByActive(boolean active);
    boolean existsByTypeAndValueIgnoreCase(EmailBlacklistType type, String value);
    boolean existsByTypeAndValueIgnoreCaseAndIdNot(EmailBlacklistType type, String value, Long id);
}
