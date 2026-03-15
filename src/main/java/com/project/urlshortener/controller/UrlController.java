package com.project.urlshortener.controller;

import com.project.urlshortener.dto.UrlRequest;
import com.project.urlshortener.dto.UrlResponse;
import com.project.urlshortener.service.UrlService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;

/**
 * REST Controller for all URL Shortener API endpoints.
 *
 * @RestController    → Combines @Controller + @ResponseBody
 *                      (all methods return JSON, not view names)
 * @RequestMapping    → Base path for all endpoints in this controller
 * @RequiredArgsConstructor → Lombok constructor injection (no @Autowired needed)
 * @Slf4j             → Lombok logger
 * @Tag               → Swagger UI grouping label
 *
 * Endpoint Summary:
 * ┌─────────────────────────────────┬──────────────────────────────────────┐
 * │ POST  /api/shorten              │ Shorten a URL                        │
 * │ GET   /{shortCode}              │ Redirect to original URL             │
 * │ GET   /api/stats/{shortCode}    │ Get analytics for a short URL        │
 * │ GET   /api/urls                 │ List all shortened URLs              │
 * │ DELETE /api/urls/{shortCode}    │ Delete a shortened URL               │
 * └─────────────────────────────────┴──────────────────────────────────────┘
 */
@RestController
@RequiredArgsConstructor
@Slf4j
@Tag(name = "URL Shortener", description = "API for creating, resolving, and managing short URLs")
public class UrlController {

    private final UrlService urlService;

    // ── 1. Shorten URL ───────────────────────────────────────────────────────

    /**
     * POST /api/shorten
     *
     * @Valid → triggers Bean Validation on UrlRequest.
     *          If validation fails, MethodArgumentNotValidException is thrown
     *          and caught by GlobalExceptionHandler → HTTP 400.
     *
     * HttpServletRequest → used to dynamically extract the base URL
     *   (e.g., "http://localhost:8080" in dev, "https://myapp.com" in prod)
     *   so we don't hardcode it.
     */
    @PostMapping("/api/shorten")
    @Operation(
            summary = "Shorten a URL",
            description = "Converts a long URL into a short one. Optionally accepts a custom alias and expiry date."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Short URL created successfully",
                    content = @Content(schema = @Schema(implementation = UrlResponse.class))),
            @ApiResponse(responseCode = "400", description = "Invalid URL or validation error"),
            @ApiResponse(responseCode = "409", description = "Custom alias already taken")
    })
    public ResponseEntity<UrlResponse> shortenUrl(
            @Valid @RequestBody UrlRequest request,
            HttpServletRequest httpRequest) {

        // Dynamically build base URL from incoming request
        String baseUrl = httpRequest.getScheme() + "://" + httpRequest.getServerName()
                + ":" + httpRequest.getServerPort();

        log.info("POST /api/shorten | url={} | alias={}", request.getUrl(), request.getCustomAlias());

        UrlResponse response = urlService.shortenUrl(request, baseUrl);

        // HTTP 201 Created is semantically correct for resource creation
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    // ── 2. Redirect ──────────────────────────────────────────────────────────

    /**
     * GET /{shortCode}
     *
     * This is the redirect endpoint. When a user visits http://localhost:8080/aB3xY:
     * 1. We resolve the short code to the original URL
     * 2. Return HTTP 302 Found with Location header = original URL
     * 3. The browser automatically follows the redirect
     *
     * HTTP 302 vs 301:
     * - 301 Permanent: browsers cache it → analytics become inaccurate
     * - 302 Temporary: no caching → every click hits our server → accurate click tracking
     *
     * ResponseEntity.status(302).location(URI.create(originalUrl)).build()
     * sets the HTTP response: Location: https://original-url.com
     */
    @GetMapping("/{shortCode:^(?!index\\.html$|favicon\\.ico$|actuator|swagger|v3|api)[a-zA-Z0-9_-]+$}")
    @Operation(
            summary = "Redirect to original URL",
            description = "Resolves the short code and redirects to the original URL (HTTP 302)"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "302", description = "Redirect to original URL"),
            @ApiResponse(responseCode = "404", description = "Short code not found"),
            @ApiResponse(responseCode = "410", description = "Short URL has expired")
    })
    public ResponseEntity<Void> redirect(
            @Parameter(description = "Short code or custom alias", example = "aB3xY")
            @PathVariable String shortCode) {

        log.info("GET /{} | Redirect request", shortCode);

        String originalUrl = urlService.resolveUrl(shortCode);

        return ResponseEntity
                .status(HttpStatus.FOUND)           // HTTP 302
                .location(URI.create(originalUrl))  // Location header
                .build();
    }

    // ── 3. Analytics ─────────────────────────────────────────────────────────

    /**
     * GET /api/stats/{shortCode}
     *
     * Returns analytics: click count, last accessed time, creation time, etc.
     */
    @GetMapping("/api/stats/{shortCode}")
    @Operation(
            summary = "Get URL analytics",
            description = "Returns click count, last accessed time, and metadata for a short URL"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Stats retrieved successfully",
                    content = @Content(schema = @Schema(implementation = UrlResponse.class))),
            @ApiResponse(responseCode = "404", description = "Short code not found")
    })
    public ResponseEntity<UrlResponse> getStats(
            @Parameter(description = "The short code to look up", example = "aB3xY")
            @PathVariable String shortCode) {

        log.info("GET /api/stats/{}", shortCode);
        return ResponseEntity.ok(urlService.getStats(shortCode));
    }

    // ── 4. List All URLs ─────────────────────────────────────────────────────

    /**
     * GET /api/urls
     *
     * Admin endpoint: returns all shortened URLs with their metadata.
     * In production, you'd secure this with Spring Security.
     */
    @GetMapping("/api/urls")
    @Operation(
            summary = "List all shortened URLs",
            description = "Returns all URL mappings in the system (admin endpoint)"
    )
    public ResponseEntity<List<UrlResponse>> getAllUrls() {
        log.info("GET /api/urls | Fetching all URLs");
        return ResponseEntity.ok(urlService.getAllUrls());
    }

    // ── 5. Delete URL ────────────────────────────────────────────────────────

    /**
     * DELETE /api/urls/{shortCode}
     *
     * Removes the URL mapping from both DB and Redis cache.
     * Returns HTTP 204 No Content (success with no body).
     */
    @DeleteMapping("/api/urls/{shortCode}")
    @Operation(
            summary = "Delete a shortened URL",
            description = "Permanently removes a short URL mapping from the system"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "URL deleted successfully"),
            @ApiResponse(responseCode = "404", description = "Short code not found")
    })
    public ResponseEntity<Void> deleteUrl(
            @Parameter(description = "The short code to delete", example = "aB3xY")
            @PathVariable String shortCode) {

        log.info("DELETE /api/urls/{}", shortCode);
        urlService.deleteUrl(shortCode);
        return ResponseEntity.noContent().build(); // HTTP 204
    }
}