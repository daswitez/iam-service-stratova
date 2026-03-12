package com.solveria.iamservice;

import com.solveria.iamservice.config.security.JwtProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.data.mongo.MongoDataAutoConfiguration;
import org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration;
import org.springframework.boot.autoconfigure.data.redis.RedisRepositoriesAutoConfiguration;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.autoconfigure.mongo.MongoAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication(
        exclude = {
            MongoAutoConfiguration.class,
            MongoDataAutoConfiguration.class,
            RedisAutoConfiguration.class,
            RedisRepositoriesAutoConfiguration.class
        })
@EntityScan(
        basePackages = {
            "com.solveria.iamservice.multitenancy.persistence.entity",
            "com.solveria.core.iam.domain.model",
            "com.solveria.core.iam.infrastructure.persistence.entity"
        })
@EnableJpaRepositories(
        basePackages = {
            "com.solveria.iamservice.multitenancy.persistence.repository",
            "com.solveria.core.iam.infrastructure.persistence.repository"
        })
@EnableConfigurationProperties(JwtProperties.class)
@ComponentScan(basePackages = {"com.solveria.iamservice", "com.solveria.core.iam"})
public class IamServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(IamServiceApplication.class, args);
    }
}
