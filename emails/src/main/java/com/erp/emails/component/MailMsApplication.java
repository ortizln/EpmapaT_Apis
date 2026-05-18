package com.erp.emails.component;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class MailMsApplication {
    public static void main(String[] args) {
        SpringApplication.run(MailMsApplication.class, args);
    }
}