package com.lekseradar.config;

import com.lekseradar.security.CurrentUser;
import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import org.springdoc.core.utils.SpringDocUtils;
import org.springframework.context.annotation.Configuration;

/**
 * Swagger UI: http://localhost:8080/swagger-ui.html
 * Нажмите «Authorize» и вставьте токен из /api/v1/auth/login (без слова Bearer).
 */
@Configuration
@OpenAPIDefinition(
        info = @Info(title = "LekseRadar API", version = "v1", description = "ИИ-репетитор: чат, упражнения, прогресс"),
        security = @SecurityRequirement(name = "bearerAuth"))
@SecurityScheme(name = "bearerAuth", type = SecuritySchemeType.HTTP, scheme = "bearer", bearerFormat = "JWT")
public class OpenApiConfig {
    static {
        // Параметр @CurrentUser подставляет Spring Security — в документации он не нужен
        SpringDocUtils.getConfig().addAnnotationsToIgnore(CurrentUser.class);
    }
}
