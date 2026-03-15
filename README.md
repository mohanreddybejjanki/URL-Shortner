# 🔗 URL Shortener Service

A URL shortening service built with Java and Spring Boot.
The system generates short codes for long URLs and redirects users to the original link.

---

## Tech Stack

* **Language:** Java 17
* **Framework:** Spring Boot
* **ORM:** Hibernate (JPA)
* **Database:** MySQL
* **Cache:** Redis
* **Build Tool:** Maven
* **Containers:** Docker + Docker Compose
* **API Documentation:** Swagger (OpenAPI)
* **Testing:** JUnit 5 + Mockito
* **Logging:** SLF4J + Logback

---

## Run the Application

The application runs locally on:

```
http://localhost:8080
```

Once running, the service can shorten URLs and redirect users through generated short codes.

---

## Local Development

Build and run the application:

```bash
mvn clean install
mvn spring-boot:run
```

Or run the packaged JAR:

```bash
java -jar target/url-shortener.jar
```

---

## API Base URL

```
http://localhost:8080
```

Example shortened link format:

```
http://localhost:8080/{shortCode}
```
