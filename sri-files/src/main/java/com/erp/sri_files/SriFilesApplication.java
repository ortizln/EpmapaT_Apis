package com.erp.sri_files;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.retry.annotation.EnableRetry;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.web.client.RestTemplate;

@SpringBootApplication
@EnableScheduling
@EnableRetry
public class SriFilesApplication {

    public static void main(String[] args) {
        SpringApplication.run(SriFilesApplication.class, args);
    }

    @Bean
    RestTemplate restTemplate(RestTemplateBuilder builder) {
        return builder.build();
    }

    static {
        org.apache.xml.security.Init.init();
        System.setProperty("org.apache.xml.security.ignoreLineBreaks", "true");
    }
}
