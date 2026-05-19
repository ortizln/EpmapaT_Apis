package com.erp.epmapaApi.config;

import jakarta.persistence.EntityManagerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.boot.orm.jpa.EntityManagerFactoryBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import javax.sql.DataSource;

@Configuration
@EnableTransactionManagement
@EnableJpaRepositories(
        basePackages = "com.erp.epmapaApi.repositories.sri",
        entityManagerFactoryRef = "sriEntityManagerFactory",
        transactionManagerRef = "sriTransactionManager"
)
public class SriDataSourceConfig {

    @Bean
    public DataSource sriDataSource(
            @Value("${sri.datasource.url}") String url,
            @Value("${sri.datasource.username}") String username,
            @Value("${sri.datasource.password}") String password,
            @Value("${sri.datasource.driver-class-name}") String driverClassName) {
        return DataSourceBuilder.create()
                .url(url)
                .username(username)
                .password(password)
                .driverClassName(driverClassName)
                .build();
    }

    @Bean
    public LocalContainerEntityManagerFactoryBean sriEntityManagerFactory(
            EntityManagerFactoryBuilder builder, @Qualifier("sriDataSource") DataSource ds) {
        return builder
                .dataSource(ds)
                .packages("com.erp.epmapaApi.models.sri")
                .persistenceUnit("sri")
                .build();
    }

    @Bean
    public PlatformTransactionManager sriTransactionManager(
            @Qualifier("sriEntityManagerFactory") EntityManagerFactory factory) {
        return new JpaTransactionManager(factory);
    }
}
