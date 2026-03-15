package com.project.urlshortener;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;

/**
 * Main entry point for the URL Shortener application.
 *
 * @EnableCaching  → enables Spring's cache abstraction (backed by Redis)
 * @SpringBootApplication → combines @Configuration + @EnableAutoConfiguration + @ComponentScan
 */
@SpringBootApplication
@EnableCaching
public class UrlShortenerApplication {

    public static void main(String[] args) {
        SpringApplication.run(UrlShortenerApplication.class, args);
    }
}