package com.erp.emails.dtos;

import com.erp.emails.model.EmailBlacklistType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public class EmailBlacklistRequest {
    @NotNull
    public EmailBlacklistType type;
    @NotBlank
    public String value;
    public String reason;
    public boolean active = true;
}
