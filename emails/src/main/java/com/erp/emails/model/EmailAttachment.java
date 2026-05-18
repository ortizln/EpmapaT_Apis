package com.erp.emails.model;

import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "email_attachment")
public class EmailAttachment {

    @Id
    @GeneratedValue
    private UUID id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "email_id")
    private EmailMessage email;

    @Column(nullable = false)
    private String filename;

    @Column(nullable = false)
    private String contentType;

    @Column(nullable = false)
    private long size;

    @Column(nullable = false, length = 800)
    private String storageRef; // ruta en disco

    @JdbcTypeCode(SqlTypes.VARBINARY)
    @Column(name = "content", columnDefinition = "bytea")
    private byte[] content;

    @Column(length = 64)
    private String sha256;

    // getters/setters ...
}
