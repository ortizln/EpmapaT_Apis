package com.erp.emails.dtos;

import com.erp.emails.model.EmailBlacklistType;

import java.time.OffsetDateTime;

public class EmailBlacklistResponse {
    public Long id;
    public EmailBlacklistType type;
    public String value;
    public String reason;
    public boolean active;
    public OffsetDateTime createdAt;
    public OffsetDateTime updatedAt;
}
