package com.lekseradar;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

// UserDetailsService не нужен: аутентификация только по JWT (убирает "Using generated security password")
@SpringBootApplication(exclude = UserDetailsServiceAutoConfiguration.class)
@ConfigurationPropertiesScan
public class LekseRadarApplication {
    public static void main(String[] args) {
        SpringApplication.run(LekseRadarApplication.class, args);
    }
} 
