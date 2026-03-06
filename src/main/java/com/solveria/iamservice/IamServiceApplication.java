package com.solveria.iamservice;

import com.solveria.iamservice.config.security.JwtProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication
@EntityScan(basePackages = "com.solveria.core.iam.domain.model")
@EnableJpaRepositories(basePackages = "com.solveria.core.iam.infrastructure.persistence.repository")
@EnableConfigurationProperties(JwtProperties.class)
public class IamServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(IamServiceApplication.class, args);
    }
}
