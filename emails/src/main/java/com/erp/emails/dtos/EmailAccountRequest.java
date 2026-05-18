package com.erp.emails.dtos;

import com.erp.emails.model.EmailAccountSecurityType;
import com.erp.emails.model.EmailAccountTransportType;
import com.erp.emails.model.EmailType;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

public class EmailAccountRequest {
    @NotBlank
    public String code;
    @NotBlank
    public String name;
    public String provider;
    @NotBlank
    public String fromAddress;
    public String fromName;
    public String replyTo;
    public String host;
    @Min(1)
    @Max(65535)
    public Integer port;
    public String protocol;
    public EmailAccountSecurityType securityType = EmailAccountSecurityType.STARTTLS;
    public EmailAccountTransportType transportType = EmailAccountTransportType.SMTP;
    public boolean authRequired = true;
    public String username;
    public String password;
    public String apiUrl;
    public String apiAuthHeader;
    public String apiAuthScheme;
    public String apiKey;
    public boolean active = true;
    public boolean defaultAccount;
    public EmailType defaultForType;
}
