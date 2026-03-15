package com.project.urlshortener.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * JPA Entity mapped to the 'urls' table in MySQL.
 *
 * Annotations explained:
 * @Entity      → marks this class as a JPA entity (Hibernate will manage it)
 * @Table       → maps to the 'urls' table; unique index on short_code for fast lookups
 * @Id          → marks the primary key
 * @GeneratedValue → auto-increment strategy (MySQL AUTO_INCREMENT)
 * @Column      → maps field to a specific column with constraints
 * @CreationTimestamp → Hibernate auto-fills this on INSERT
 *
 * Lombok annotations:
 * @Data        → generates getters, setters, toString, equals, hashCode
 * @Builder     → enables builder pattern: Url.builder().originalUrl("...").build()
 * @NoArgsConstructor / @AllArgsConstructor → required by JPA + Builder
 */
@Entity
@Table(name = "urls", indexes = {
        @Index(name = "idx_short_code", columnList = "short_code", unique = true)
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Url {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * The original long URL submitted by the user.
     * columnDefinition = "TEXT" allows URLs longer than VARCHAR(255)
     */
    @Column(name = "original_url", nullable = false, columnDefinition = "TEXT")
    private String originalUrl;

    /**
     * The unique short code (e.g., "aB3xY").
     * Indexed and unique for O(1) lookup.
     */
    @Column(name = "short_code", nullable = false, unique = true, length = 20)
    private String shortCode;

    /**
     * Optional custom alias provided by the user (e.g., "chatgpt").
     */
    @Column(name = "custom_alias", unique = true, length = 50)
    private String customAlias;

    /**
     * Auto-populated timestamp when the record is first saved.
     */
    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    /**
     * Optional expiry datetime. After this, the short URL returns 410 Gone.
     */
    @Column(name = "expiry_date")
    private LocalDateTime expiryDate;

    /**
     * Total number of times this short URL has been accessed.
     * Starts at 0, incremented on every redirect.
     */
    @Column(name = "click_count", nullable = false)
    @Builder.Default
    private Long clickCount = 0L;

    /**
     * Timestamp of the most recent redirect access.
     */
    @Column(name = "last_accessed")
    private LocalDateTime lastAccessed;

    /**
     * Convenience method: checks if this URL has passed its expiry date.
     */
    public boolean isExpired() {
        return expiryDate != null && LocalDateTime.now().isAfter(expiryDate);
    }
}