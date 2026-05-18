package com.erp.bandred;

import jakarta.annotation.PostConstruct;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import java.util.TimeZone;

@SpringBootApplication
public class BandredApplication {

	@PostConstruct
	public void initTimezone() {
		TimeZone.setDefault(TimeZone.getTimeZone("America/Guayaquil"));
	}

	public static void main(String[] args) {
		SpringApplication.run(BandredApplication.class, args);
	}

}
