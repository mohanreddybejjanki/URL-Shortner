package com.project.urlshortener.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;
import org.hibernate.validator.constraints.URL;

import java.time.LocalDateTime;

/**
 * DTO (Data Transfer Object) for incoming shorten-URL requests.
 *
 * Why use a DTO instead of the Entity directly?
 * → Separation of concerns: the API contract is independent of the DB schema.
 * → Prevents over-posting attacks (users cannot set id, clickCount, etc.).
 * → Validation annotations live here, keeping the Entity clean.
 *
 * Validation annotations:
 * @NotBlank → field must not be null or whitespace
 * @URL      → validates proper URL format (hibernate-validator)
 * @Size     → limits string length
 * @Pattern  → regex constraint (alphanumeric alias only)
 */
@Data
@Schema(description = "Request body for creating a shortened URL")
public class UrlRequest {

    /**
     * The original long URL to shorten.
     * Example: https://www.google.com/search?q=spring+boot+tutorial
     */
    @NotBlank(message = "URL must not be blank")
    @URL(message = "Please provide a valid URL starting with http:// or https://")
    @Schema(
            description = "The original long URL to shorten",
            example = "https://www.google.com/search?q=spring+boot+tutorial"
    )
    private String url;

    /**
     * Optional: user-defined alias for the short URL.
     * If provided, the short URL will be: http://localhost:8080/{customAlias}
     * Must be 3–30 alphanumeric characters (hyphens allowed).
     */
    @Size(min = 3, max = 30, message = "Custom alias must be between 3 and 30 characters")
    @Pattern(
            regexp = "^[a-zA-Z0-9-]*$",
            message = "Custom alias can only contain letters, numbers, and hyphens"
    )
    @Schema(
            description = "Optional custom alias (e.g., 'my-link')",
            example = "my-link",
            required = false
    )
    private String customAlias;

    /**
     * Optional: expiry date/time for this short URL.
     * After this datetime, accessing the short URL returns HTTP 410 Gone.
     */
    @Schema(
            description = "Optional expiry datetime (ISO format). Null = never expires.",
            example = "2025-12-31T23:59:59"
    )
    private LocalDateTime expiryDate;
}