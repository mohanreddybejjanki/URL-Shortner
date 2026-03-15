package com.project.urlshortener.service;

import com.project.urlshortener.dto.UrlRequest;
import com.project.urlshortener.dto.UrlResponse;
import com.project.urlshortener.exception.DuplicateAliasException;
import com.project.urlshortener.exception.UrlExpiredException;
import com.project.urlshortener.exception.UrlNotFoundException;
import com.project.urlshortener.model.Url;
import com.project.urlshortener.repository.UrlRepository;
import com.project.urlshortener.util.ShortCodeGenerator;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Core business logic for URL shortening.
 *
 * Key Spring annotations:
 * @Service        → marks as a Spring-managed service bean
 * @RequiredArgsConstructor → Lombok: generates constructor for final fields (= constructor injection)
 * @Slf4j          → Lombok: injects a Logger named 'log'
 * @Transactional  → wraps methods in a DB transaction; rolls back on unchecked exceptions
 *
 * Caching strategy:
 * @Cacheable("urls") → On first call, fetches from DB and stores in Redis with key=shortCode.
 *                      Subsequent calls skip DB and read from Redis (sub-millisecond).
 * @CacheEvict        → When a URL is deleted, removes its Redis entry to prevent stale data.
 *
 * Redis TTL is configured in RedisConfig (default: 1 hour).
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class UrlServiceImpl implements UrlService {

    private final UrlRepository urlRepository;
    private final ShortCodeGenerator shortCodeGenerator;

    // Maximum attempts to generate a unique short code before giving up
    private static final int MAX_COLLISION_RETRIES = 5;

    // ── Shorten URL ─────────────────────────────────────────────────────────

    /**
     * Main method: validates, generates code, saves to DB, returns response.
     *
     * Flow:
     * 1. If customAlias provided → validate it's not taken
     * 2. Else → generate Base62 code, retry if collision
     * 3. Build Url entity → save to MySQL
     * 4. Map to UrlResponse → return
     */
    @Override
    @Transactional
    public UrlResponse shortenUrl(UrlRequest request, String baseUrl) {
        log.info("Shortening URL: {}", request.getUrl());

        String shortCode;

        if (request.getCustomAlias() != null && !request.getCustomAlias().isBlank()) {
            // ── Custom Alias Path ──────────────────────────────────────────
            String alias = request.getCustomAlias().trim();

            if (urlRepository.existsByCustomAlias(alias)) {
                throw new DuplicateAliasException(
                        "Custom alias '" + alias + "' is already taken. Please choose another.");
            }
            shortCode = alias;
            log.info("Using custom alias: {}", shortCode);

        } else {
            // ── Auto-Generate Path ─────────────────────────────────────────
            shortCode = generateUniqueCode();
            log.info("Generated short code: {}", shortCode);
        }

        // Build the entity using Lombok @Builder
        Url url = Url.builder()
                .originalUrl(request.getUrl())
                .shortCode(shortCode)
                .customAlias(request.getCustomAlias())
                .expiryDate(request.getExpiryDate())
                .clickCount(0L)
                .build();

        Url savedUrl = urlRepository.save(url);
        log.info("Saved URL with id={}, shortCode={}", savedUrl.getId(), savedUrl.getShortCode());

        return mapToResponse(savedUrl, baseUrl);
    }

    // ── Resolve / Redirect ───────────────────────────────────────────────────

    /**
     * Resolves a short code to the original URL.
     *
     * @Cacheable("urls") → Redis key = "urls::<shortCode>"
     * On cache hit: returns originalUrl without touching MySQL.
     * On cache miss: queries MySQL, stores result in Redis, returns originalUrl.
     *
     * @Transactional is needed because we call the @Modifying incrementClickCount query.
     */
    @Override
    @Transactional
    @Cacheable(value = "urls", key = "#code")
    public String resolveUrl(String code) {
        log.info("Resolving short code: {}", code);

        // Try shortCode first, then customAlias
        Url url = urlRepository.findByShortCode(code)
                .or(() -> urlRepository.findByCustomAlias(code))
                .orElseThrow(() -> new UrlNotFoundException(
                        "Short URL '" + code + "' not found."));

        // Check expiry
        if (url.isExpired()) {
            throw new UrlExpiredException(
                    "Short URL '" + code + "' has expired on " + url.getExpiryDate());
        }

        // Atomically increment click count (no race condition)
        urlRepository.incrementClickCount(url.getShortCode());

        log.info("Redirecting {} → {}", code, url.getOriginalUrl());
        return url.getOriginalUrl();
    }

    // ── Analytics ────────────────────────────────────────────────────────────

    @Override
    public UrlResponse getStats(String shortCode) {
        Url url = urlRepository.findByShortCode(shortCode)
                .orElseThrow(() -> new UrlNotFoundException(
                        "No stats found for short code: " + shortCode));

        // Use "stats" base path for the response (no redirect host needed)
        return mapToResponse(url, "http://localhost:8080");
    }

    // ── Get All ──────────────────────────────────────────────────────────────

    @Override
    public List<UrlResponse> getAllUrls() {
        return urlRepository.findAll()
                .stream()
                .map(url -> mapToResponse(url, "http://localhost:8080"))
                .collect(Collectors.toList());
    }

    // ── Delete ───────────────────────────────────────────────────────────────

    /**
     * @CacheEvict → removes the Redis entry for this shortCode when deleted.
     * Without this, deleted URLs could still resolve from cache.
     */
    @Override
    @Transactional
    @CacheEvict(value = "urls", key = "#shortCode")
    public void deleteUrl(String shortCode) {
        Url url = urlRepository.findByShortCode(shortCode)
                .orElseThrow(() -> new UrlNotFoundException(
                        "Cannot delete: short code '" + shortCode + "' not found."));

        urlRepository.delete(url);
        log.info("Deleted URL with shortCode={}", shortCode);
    }

    // ── Private Helpers ──────────────────────────────────────────────────────

    /**
     * Generates a unique Base62 short code with collision retry.
     * In practice, collision is extremely rare (1 in 56 billion for 6-char codes),
     * but we handle it gracefully.
     */
    private String generateUniqueCode() {
        for (int attempt = 0; attempt < MAX_COLLISION_RETRIES; attempt++) {
            String code = shortCodeGenerator.generate();
            if (!urlRepository.existsByShortCode(code)) {
                return code;
            }
            log.warn("Short code collision on '{}', retrying... (attempt {})", code, attempt + 1);
        }
        throw new RuntimeException("Failed to generate unique short code after "
                + MAX_COLLISION_RETRIES + " attempts.");
    }

    /**
     * Maps a Url entity to a UrlResponse DTO.
     * Constructs the full short URL: baseUrl + "/" + shortCode
     */
    private UrlResponse mapToResponse(Url url, String baseUrl) {
        return UrlResponse.builder()
                .shortUrl(baseUrl + "/" + url.getShortCode())
                .originalUrl(url.getOriginalUrl())
                .shortCode(url.getShortCode())
                .customAlias(url.getCustomAlias())
                .createdAt(url.getCreatedAt())
                .expiryDate(url.getExpiryDate())
                .clickCount(url.getClickCount())
                .lastAccessed(url.getLastAccessed())
                .build();
    }
}