package com.erp.sri_files.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@Configuration
@EnableConfigurationProperties(SriFilesProperties.class)
public class StorageConfig {
}
