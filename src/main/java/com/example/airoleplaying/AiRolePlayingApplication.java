package com.example.airoleplaying;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

/**
 * AI角色扮演系统主应用类
 * 
 * @author AI Assistant
 * @since 1.0.0
 */
@SpringBootApplication
@ConfigurationPropertiesScan
public class AiRolePlayingApplication {

    public static void main(String[] args) {
        SpringApplication.run(AiRolePlayingApplication.class, args);
    }
}
