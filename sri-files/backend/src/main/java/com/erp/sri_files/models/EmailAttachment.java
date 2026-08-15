package com.erp.sri_files.models;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class EmailAttachment {
    private String fileName;
    private String contentType;
    private byte[] content;
}