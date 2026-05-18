package com.erp.sri_files.dto;

public record EmailMsAttachmentInput(
        String name,
        String contentType,
        String base64
) {}
