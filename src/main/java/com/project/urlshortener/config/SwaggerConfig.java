package com.project.urlshortener.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * OpenAPI 3.0 / Swagger UI Configuration.
 *
 * After starting the app, visit:
 *   Swagger UI  → http://localhost:8080/swagger-ui/index.html
 *   OpenAPI JSON → http://localhost:8080/v3/api-docs
 *
 * Swagger lets you:
 * → Explore all endpoints visually
 * → Send test requests directly from the browser
 * → Share API documentation with frontend teams
 */
@Configuration
public class SwaggerConfig {

    @Bean
    public OpenAPI urlShortenerOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("URL Shortener API")
                        .description("""
                                A production-grade URL Shortening Service built with Spring Boot.
                                
                                **Features:**
                                - Shorten long URLs with auto-generated Base62 codes
                                - Custom alias support (e.g., `/chatgpt`)
                                - Expiry date for time-limited links
                                - Click analytics (total clicks, last accessed)
                                - Redis caching for fast redirects
                                """)
                        .version("1.0.0")
                        .contact(new Contact()
                                .name("URL Shortener Project")
                                .email("dev@urlshortener.com"))
                        .license(new License()
                                .name("MIT License")))
                .servers(List.of(
                        new Server()
                                .url("http://localhost:8080")
                                .description("Local Development Server")
                ));
    }
}