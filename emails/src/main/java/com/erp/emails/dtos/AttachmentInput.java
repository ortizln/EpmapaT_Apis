package com.erp.emails.dtos;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public class AttachmentInput {
    @NotBlank public String name;
    @NotBlank public String contentType;
    @NotNull public String base64;
}