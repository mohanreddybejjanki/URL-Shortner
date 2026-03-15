package com.project.urlshortener.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * DTO returned in API responses.
 *
 * This decouples what we expose to the client from the internal Entity.
 * For example, we never expose the internal database 'id'.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Response body containing the shortened URL and metadata")
public class UrlResponse {

    @Schema(description = "The generated short URL", example = "http://localhost:8080/aB3xY")
    private String shortUrl;

    @Schema(description = "The original long URL", example = "https://www.google.com/search?q=spring+boot")
    private String originalUrl;

    @Schema(description = "The short code part of the URL", example = "aB3xY")
    private String shortCode;

    @Schema(description = "Optional custom alias if provided", example = "my-link")
    private String customAlias;

    @Schema(description = "When the short URL was created")
    private LocalDateTime createdAt;

    @Schema(description = "When the short URL expires (null = never)", example = "2025-12-31T23:59:59")
    private LocalDateTime expiryDate;

    @Schema(description = "Total number of redirects via this short URL", example = "42")
    private Long clickCount;

    @Schema(description = "Last time this short URL was accessed")
    private LocalDateTime lastAccessed;
}