package com.erp.emails.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "email_message")
public class EmailMessage {

    @Id
    @GeneratedValue
    private UUID id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EmailType type;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EmailStatus status;

    @Column(nullable = false, length = 4000)
    private String toRecipients; // CSV

    @Column(length = 4000)
    private String ccRecipients;

    @Column(length = 4000)
    private String bccRecipients;

    @Column(nullable = false, length = 500)
    private String subject;

    @Lob
    private String bodyHtml;

    @Lob
    private String bodyText;

    @Column(length = 200)
    private String correlationId;

    @ManyToOne
    @JoinColumn(name = "email_account_id")
    private EmailAccount account;

    @Column(length = 320)
    private String fromAddress;

    @Column(nullable = false)
    private int attempts;

    @Column(length = 2000)
    private String lastError;

    private OffsetDateTime createdAt;
    private OffsetDateTime sentAt;

    // getters/setters

    @PrePersist
    void onCreate() {
        createdAt = OffsetDateTime.now();
        if (status == null) status = EmailStatus.PENDING;
    }

    // Getters/Setters omitidos por brevedad (puedes usar Lombok @Data)
    // ...
}
