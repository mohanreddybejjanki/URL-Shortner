package com.project.urlshortener.repository;

import com.project.urlshortener.model.Url;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Spring Data JPA Repository for the Url entity.
 *
 * Extending JpaRepository<Url, Long> gives us for FREE:
 *   save(), findById(), findAll(), deleteById(), count(), existsById(), etc.
 *
 * We only need to declare custom queries here.
 *
 * Spring Data JPA automatically generates the SQL for method names like:
 *   findByShortCode → SELECT * FROM urls WHERE short_code = ?
 *   existsByShortCode → SELECT COUNT(*) > 0 FROM urls WHERE short_code = ?
 */
@Repository
public interface UrlRepository extends JpaRepository<Url, Long> {

    /**
     * Find a URL record by its short code.
     * Used during redirect: GET /{shortCode}
     */
    Optional<Url> findByShortCode(String shortCode);

    /**
     * Find a URL record by custom alias.
     * Used to validate uniqueness and for redirect lookup.
     */
    Optional<Url> findByCustomAlias(String customAlias);

    /**
     * Check if a short code already exists (for uniqueness guarantee).
     */
    boolean existsByShortCode(String shortCode);

    /**
     * Check if a custom alias is already taken.
     */
    boolean existsByCustomAlias(String customAlias);

    /**
     * Atomically increment click count and update last_accessed.
     *
     * @Modifying → tells Spring this is an UPDATE/DELETE, not a SELECT
     * @Query     → custom JPQL query (not SQL — uses entity field names)
     *
     * Why do this instead of load-update-save?
     * → Avoids race conditions when multiple users click simultaneously.
     * → Single DB round-trip instead of three.
     */
    @Modifying
    @Query("UPDATE Url u SET u.clickCount = u.clickCount + 1, " +
            "u.lastAccessed = CURRENT_TIMESTAMP WHERE u.shortCode = :shortCode")
    void incrementClickCount(@Param("shortCode") String shortCode);
}