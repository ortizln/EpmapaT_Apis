package com.erp.emails.service;

public class EmailBlacklistViolationException extends IllegalArgumentException {
    public EmailBlacklistViolationException(String message) {
        super(message);
    }
}
