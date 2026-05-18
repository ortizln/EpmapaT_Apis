package com.erp.emails.component;

import com.erp.emails.model.EmailMessage;
import com.erp.emails.model.EmailStatus;
import com.erp.emails.model.EmailType;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

public class EmailSpecs {
    public static Specification<EmailMessage> filter(
            EmailStatus status,
            EmailType type,
            String correlationId,
            Long accountId,
            String search,
            LocalDate createdFrom,
            LocalDate createdTo
    ) {
        return (root, query, cb) -> {
            var p = cb.conjunction();
            if (status != null) p.getExpressions().add(cb.equal(root.get("status"), status));
            if (type != null) p.getExpressions().add(cb.equal(root.get("type"), type));
            if (correlationId != null && !correlationId.isBlank()) {
                p.getExpressions().add(cb.like(cb.lower(root.get("correlationId")), "%" + correlationId.trim().toLowerCase() + "%"));
            }
            if (accountId != null) {
                p.getExpressions().add(cb.equal(root.get("account").get("id"), accountId));
            }
            if (search != null && !search.isBlank()) {
                String pattern = "%" + search.trim().toLowerCase() + "%";
                p.getExpressions().add(cb.or(
                        cb.like(cb.lower(root.get("subject")), pattern),
                        cb.like(cb.lower(root.get("toRecipients")), pattern),
                        cb.like(cb.lower(root.get("fromAddress")), pattern),
                        cb.like(cb.lower(root.get("correlationId")), pattern)
                ));
            }
            if (createdFrom != null) {
                p.getExpressions().add(cb.greaterThanOrEqualTo(
                        root.get("createdAt"),
                        createdFrom.atStartOfDay().atOffset(ZoneOffset.UTC)
                ));
            }
            if (createdTo != null) {
                p.getExpressions().add(cb.lessThan(
                        root.get("createdAt"),
                        createdTo.plusDays(1).atStartOfDay().atOffset(ZoneOffset.UTC)
                ));
            }
            return p;
        };
    }

    public static Specification<EmailMessage> pendingStale(OffsetDateTime createdBefore, Integer minAttempts) {
        return (root, query, cb) -> {
            var p = cb.conjunction();
            p.getExpressions().add(cb.equal(root.get("status"), EmailStatus.PENDING));
            if (createdBefore != null) {
                p.getExpressions().add(cb.lessThanOrEqualTo(root.get("createdAt"), createdBefore));
            }
            if (minAttempts != null) {
                p.getExpressions().add(cb.greaterThanOrEqualTo(root.get("attempts"), minAttempts));
            }
            return p;
        };
    }
}
