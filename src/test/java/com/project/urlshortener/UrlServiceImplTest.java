package com.project.urlshortener;

import com.project.urlshortener.dto.UrlRequest;
import com.project.urlshortener.dto.UrlResponse;
import com.project.urlshortener.exception.DuplicateAliasException;
import com.project.urlshortener.exception.UrlExpiredException;
import com.project.urlshortener.exception.UrlNotFoundException;
import com.project.urlshortener.model.Url;
import com.project.urlshortener.repository.UrlRepository;
import com.project.urlshortener.service.UrlServiceImpl;
import com.project.urlshortener.util.ShortCodeGenerator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit Tests for UrlServiceImpl.
 *
 * Testing strategy:
 * → @ExtendWith(MockitoExtension.class) → enables Mockito without Spring context (fast!)
 * → @Mock → creates mock objects (no real DB or Redis needed)
 * → @InjectMocks → creates UrlServiceImpl and injects the mocks
 * → We test the SERVICE layer in isolation
 *
 * Test naming convention: methodName_scenario_expectedResult
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("UrlService Unit Tests")
class UrlServiceImplTest {

    @Mock
    private UrlRepository urlRepository;

    @Mock
    private ShortCodeGenerator shortCodeGenerator;

    @InjectMocks
    private UrlServiceImpl urlService;

    private static final String BASE_URL = "http://localhost:8080";
    private static final String SHORT_CODE = "aB3xY2";
    private static final String ORIGINAL_URL = "https://www.google.com/very-long-url";

    private Url sampleUrl;

    @BeforeEach
    void setUp() {
        sampleUrl = Url.builder()
                .id(1L)
                .originalUrl(ORIGINAL_URL)
                .shortCode(SHORT_CODE)
                .clickCount(0L)
                .createdAt(LocalDateTime.now())
                .build();
    }

    // ── shortenUrl tests ──────────────────────────────────────────────────────

    @Test
    @DisplayName("shortenUrl: should generate short code and return response")
    void shortenUrl_validRequest_returnsUrlResponse() {
        // Arrange
        UrlRequest request = new UrlRequest();
        request.setUrl(ORIGINAL_URL);

        when(shortCodeGenerator.generate()).thenReturn(SHORT_CODE);
        when(urlRepository.existsByShortCode(SHORT_CODE)).thenReturn(false);
        when(urlRepository.save(any(Url.class))).thenReturn(sampleUrl);

        // Act
        UrlResponse response = urlService.shortenUrl(request, BASE_URL);

        // Assert
        assertThat(response).isNotNull();
        assertThat(response.getShortUrl()).isEqualTo(BASE_URL + "/" + SHORT_CODE);
        assertThat(response.getOriginalUrl()).isEqualTo(ORIGINAL_URL);

        verify(urlRepository, times(1)).save(any(Url.class));
    }

    @Test
    @DisplayName("shortenUrl: with custom alias should use alias as short code")
    void shortenUrl_customAlias_usesAliasAsShortCode() {
        // Arrange
        String alias = "my-link";
        UrlRequest request = new UrlRequest();
        request.setUrl(ORIGINAL_URL);
        request.setCustomAlias(alias);

        Url urlWithAlias = Url.builder()
                .id(1L)
                .originalUrl(ORIGINAL_URL)
                .shortCode(alias)
                .customAlias(alias)
                .clickCount(0L)
                .createdAt(LocalDateTime.now())
                .build();

        when(urlRepository.existsByCustomAlias(alias)).thenReturn(false);
        when(urlRepository.save(any(Url.class))).thenReturn(urlWithAlias);

        // Act
        UrlResponse response = urlService.shortenUrl(request, BASE_URL);

        // Assert
        assertThat(response.getShortUrl()).isEqualTo(BASE_URL + "/" + alias);
        verify(shortCodeGenerator, never()).generate(); // should NOT auto-generate
    }

    @Test
    @DisplayName("shortenUrl: duplicate alias should throw DuplicateAliasException")
    void shortenUrl_duplicateAlias_throwsDuplicateAliasException() {
        // Arrange
        UrlRequest request = new UrlRequest();
        request.setUrl(ORIGINAL_URL);
        request.setCustomAlias("taken-alias");

        when(urlRepository.existsByCustomAlias("taken-alias")).thenReturn(true);

        // Act & Assert
        assertThatThrownBy(() -> urlService.shortenUrl(request, BASE_URL))
                .isInstanceOf(DuplicateAliasException.class)
                .hasMessageContaining("taken-alias");
    }

    // ── resolveUrl tests ──────────────────────────────────────────────────────

    @Test
    @DisplayName("resolveUrl: valid code should return original URL")
    void resolveUrl_validCode_returnsOriginalUrl() {
        // Arrange
        when(urlRepository.findByShortCode(SHORT_CODE)).thenReturn(Optional.of(sampleUrl));
        doNothing().when(urlRepository).incrementClickCount(SHORT_CODE);

        // Act
        String result = urlService.resolveUrl(SHORT_CODE);

        // Assert
        assertThat(result).isEqualTo(ORIGINAL_URL);
        verify(urlRepository).incrementClickCount(SHORT_CODE);
    }

    @Test
    @DisplayName("resolveUrl: unknown code should throw UrlNotFoundException")
    void resolveUrl_unknownCode_throwsNotFoundException() {
        // Arrange
        when(urlRepository.findByShortCode("unknown")).thenReturn(Optional.empty());
        when(urlRepository.findByCustomAlias("unknown")).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> urlService.resolveUrl("unknown"))
                .isInstanceOf(UrlNotFoundException.class);
    }

    @Test
    @DisplayName("resolveUrl: expired URL should throw UrlExpiredException")
    void resolveUrl_expiredUrl_throwsUrlExpiredException() {
        // Arrange - set expiry date in the past
        Url expiredUrl = Url.builder()
                .originalUrl(ORIGINAL_URL)
                .shortCode(SHORT_CODE)
                .clickCount(0L)
                .expiryDate(LocalDateTime.now().minusDays(1)) // yesterday
                .build();

        when(urlRepository.findByShortCode(SHORT_CODE)).thenReturn(Optional.of(expiredUrl));

        // Act & Assert
        assertThatThrownBy(() -> urlService.resolveUrl(SHORT_CODE))
                .isInstanceOf(UrlExpiredException.class);
    }

    // ── deleteUrl tests ───────────────────────────────────────────────────────

    @Test
    @DisplayName("deleteUrl: valid code should delete the URL")
    void deleteUrl_validCode_deletesSuccessfully() {
        // Arrange
        when(urlRepository.findByShortCode(SHORT_CODE)).thenReturn(Optional.of(sampleUrl));
        doNothing().when(urlRepository).delete(sampleUrl);

        // Act
        urlService.deleteUrl(SHORT_CODE);

        // Assert
        verify(urlRepository, times(1)).delete(sampleUrl);
    }

    @Test
    @DisplayName("deleteUrl: unknown code should throw UrlNotFoundException")
    void deleteUrl_unknownCode_throwsNotFoundException() {
        when(urlRepository.findByShortCode("xyz")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> urlService.deleteUrl("xyz"))
                .isInstanceOf(UrlNotFoundException.class);
    }
}