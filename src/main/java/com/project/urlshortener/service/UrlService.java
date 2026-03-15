package com.project.urlshortener.service;

import com.project.urlshortener.dto.UrlRequest;
import com.project.urlshortener.dto.UrlResponse;
import com.project.urlshortener.model.Url;

import java.util.List;

/**
 * Service interface defining all URL Shortener business operations.
 *
 * Why define an interface?
 * → Supports Dependency Inversion Principle (SOLID).
 * → Easy to mock in unit tests (@MockBean / Mockito.mock(UrlService.class)).
 * → Allows multiple implementations (e.g., UrlServiceImpl for prod, MockUrlService for tests).
 * → Controllers depend on the abstraction, not the concrete class.
 */
public interface UrlService {

    /**
     * Shorten a URL. Handles both auto-generated and custom alias codes.
     *
     * @param request UrlRequest with original URL, optional alias, optional expiry
     * @param baseUrl the server base URL (e.g., "http://localhost:8080") injected from request
     * @return UrlResponse with the full short URL and metadata
     */
    UrlResponse shortenUrl(UrlRequest request, String baseUrl);

    /**
     * Resolve a short code to the original URL and record the click.
     *
     * @param code the shortCode or customAlias
     * @return the original long URL for redirect
     * @throws com.project.urlshortener.exception.UrlNotFoundException if code doesn't exist
     * @throws com.project.urlshortener.exception.UrlExpiredException  if URL has expired
     */
    String resolveUrl(String code);

    /**
     * Retrieve analytics/stats for a given short code.
     *
     * @param shortCode the short code to look up
     * @return UrlResponse with click count, last accessed, etc.
     */
    UrlResponse getStats(String shortCode);

    /**
     * Retrieve all shortened URLs (admin/listing endpoint).
     */
    List<UrlResponse> getAllUrls();

    /**
     * Delete a shortened URL by its short code.
     *
     * @param shortCode the short code to delete
     */
    void deleteUrl(String shortCode);
}