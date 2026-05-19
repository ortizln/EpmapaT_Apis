package com.erp.epmapaApi.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

@Configuration
public class CorsConfig {

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration cfg = new CorsConfiguration();
        cfg.addAllowedOriginPattern("*");
        cfg.addAllowedMethod(CorsConfiguration.ALL);
        cfg.addAllowedHeader(CorsConfiguration.ALL);
        cfg.addExposedHeader("*");
        cfg.setAllowCredentials(false);
        cfg.setMaxAge(86400 * 365L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", cfg);
        return source;
    }
}
