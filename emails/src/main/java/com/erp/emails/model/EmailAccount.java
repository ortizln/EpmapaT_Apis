package com.erp.emails.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.OffsetDateTime;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Table(
        name = "email_account",
        uniqueConstraints = {
                @UniqueConstraint(name = "email_account_code_uk", columnNames = "code")
        }
)
public class EmailAccount {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 50)
    private String code;

    @Column(nullable = false, length = 150)
    private String name;

    @Column(length = 100)
    private String provider;

    @Column(nullable = false, length = 320)
    private String fromAddress;

    @Column(length = 150)
    private String fromName;

    @Column(length = 320)
    private String replyTo;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EmailAccountTransportType transportType;

    @Column(length = 150)
    private String host;

    @Column
    private Integer port;

    @Column(length = 20)
    private String protocol;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private EmailAccountSecurityType securityType;

    @Column(nullable = false)
    private boolean authRequired;

    @Column(length = 320)
    private String username;

    @Column(length = 500)
    private String password;

    @Column(length = 500)
    private String apiUrl;

    @Column(length = 100)
    private String apiAuthHeader;

    @Column(length = 50)
    private String apiAuthScheme;

    @Column(length = 500)
    private String apiKey;

    @Column(nullable = false)
    private boolean active;

    @Column(name = "is_default", nullable = false)
    private boolean defaultAccount;

    @Enumerated(EnumType.STRING)
    @Column(length = 30)
    private EmailType defaultForType;

    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;

    @PrePersist
    void onCreate() {
        if (transportType == null) transportType = EmailAccountTransportType.SMTP;
        if (protocol == null || protocol.isBlank()) protocol = "smtp";
        if (securityType == null) securityType = EmailAccountSecurityType.STARTTLS;
        createdAt = OffsetDateTime.now();
        updatedAt = createdAt;
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = OffsetDateTime.now();
    }
}
