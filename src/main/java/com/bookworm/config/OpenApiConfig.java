package com.bookworm.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {
    @Bean
    public OpenAPI bookstoreOpenApi() {
        return new OpenAPI()
                .info(new Info().title("Book Worm E-Bookstore API").version("1.0.0")
                        .description("Backend for browsing, buying and managing books. "
                                + "Login via /api/auth/login, then send 'Authorization: Bearer <token>'."))
                .components(new Components().addSecuritySchemes("bearerAuth",
                        new SecurityScheme().type(SecurityScheme.Type.HTTP).scheme("bearer")))
                .addSecurityItem(new SecurityRequirement().addList("bearerAuth"));
    }
}
