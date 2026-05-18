package com.erp.emails;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class EmailsApplication {

	public static void main(String[] args) {
		SpringApplication.run(EmailsApplication.class, args);
	}

}
