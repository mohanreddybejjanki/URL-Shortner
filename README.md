# 🔗 URL Shortener Service

A **production-grade URL Shortening Service** built with Java, Spring Boot, MySQL, Redis, and Docker.

> **For Btech Freshers** — Every line of code is commented. Every decision is explained. Read the code like a textbook.

---

## 📋 Table of Contents

1. [Tech Stack](#tech-stack)
2. [Features](#features)
3. [Project Structure](#project-structure)
4. [Database Design](#database-design)
5. [API Reference](#api-reference)
6. [Setup in IntelliJ IDEA (Step by Step)](#setup-in-intellij-idea)
7. [Terminal Commands to Create Project](#terminal-commands)
8. [Running Without Docker](#running-without-docker)
9. [Running With Docker](#running-with-docker)
10. [Testing with Postman](#testing-with-postman)
11. [How Each Concept Works](#how-each-concept-works)
12. [Interview Q&A](#interview-qa)

---

## Tech Stack

| Layer | Technology | Why? |
|-------|-----------|------|
| Language | Java 17 | LTS version, modern features (records, text blocks) |
| Framework | Spring Boot 3.2 | Auto-configuration, reduces boilerplate |
| ORM | Hibernate (via JPA) | Maps Java objects ↔ DB tables automatically |
| Database | MySQL 8 | Reliable, widely used in industry |
| Cache | Redis 7 | In-memory, microsecond reads |
| Build Tool | Maven | Dependency management, project lifecycle |
| Containers | Docker + Docker Compose | Reproducible environments |
| API Docs | Swagger (SpringDoc OpenAPI 3) | Interactive API documentation |
| Testing | JUnit 5 + Mockito | Unit testing without real DB |
| Logging | SLF4J + Logback | Industry-standard logging |

---

## Features

### Core
- ✅ Shorten any valid URL → unique 6-character Base62 code
- ✅ Redirect short URL → original URL (HTTP 302)
- ✅ Custom alias (e.g., `/chatgpt`, `/my-link`)
- ✅ URL validation (rejects invalid URLs)

### Advanced
- ✅ Expiry date support (returns HTTP 410 Gone after expiry)
- ✅ Click tracking (total clicks + last accessed timestamp)
- ✅ Redis caching (cache hit skips MySQL entirely)
- ✅ Global exception handler (consistent error responses)
- ✅ Swagger UI (interactive API explorer)
- ✅ Docker Compose (one command to run everything)
- ✅ Unit tests (JUnit 5 + Mockito)
- ✅ Actuator health endpoint

---

## Project Structure

```
url-shortener/
│
├── src/main/java/com/project/urlshortener/
│   │
│   ├── UrlShortenerApplication.java      ← Entry point (@SpringBootApplication)
│   │
│   ├── controller/
│   │   └── UrlController.java            ← REST endpoints (HTTP in/out)
│   │
│   ├── service/
│   │   ├── UrlService.java               ← Interface (contract)
│   │   └── UrlServiceImpl.java           ← Business logic implementation
│   │
│   ├── repository/
│   │   └── UrlRepository.java            ← DB queries (Spring Data JPA)
│   │
│   ├── model/
│   │   └── Url.java                      ← JPA Entity (maps to 'urls' table)
│   │
│   ├── dto/
│   │   ├── UrlRequest.java               ← Incoming request body
│   │   └── UrlResponse.java              ← Outgoing response body
│   │
│   ├── util/
│   │   └── ShortCodeGenerator.java       ← Base62 encoding logic
│   │
│   ├── exception/
│   │   ├── UrlNotFoundException.java     ← HTTP 404
│   │   ├── UrlExpiredException.java      ← HTTP 410
│   │   ├── DuplicateAliasException.java  ← HTTP 409
│   │   └── GlobalExceptionHandler.java  ← Catches all exceptions
│   │
│   └── config/
│       ├── RedisConfig.java              ← Redis cache configuration
│       └── SwaggerConfig.java            ← OpenAPI documentation setup
│
├── src/main/resources/
│   └── application.properties            ← All configuration
│
├── src/test/java/com/project/urlshortener/
│   └── UrlServiceImplTest.java           ← Unit tests
│
├── Dockerfile                            ← Container build instructions
├── docker-compose.yml                    ← Multi-service orchestration
├── pom.xml                               ← Maven dependencies
└── README.md
```

---

## Database Design

### Table: `urls`

```sql
CREATE TABLE urls (
    id           BIGINT AUTO_INCREMENT PRIMARY KEY,
    original_url TEXT         NOT NULL,
    short_code   VARCHAR(20)  NOT NULL UNIQUE,
    custom_alias VARCHAR(50)  UNIQUE,
    created_at   TIMESTAMP    DEFAULT CURRENT_TIMESTAMP,
    expiry_date  TIMESTAMP    NULL,
    click_count  BIGINT       DEFAULT 0,
    last_accessed TIMESTAMP   NULL,
    
    INDEX idx_short_code (short_code)   -- Fast lookup on redirect
);
```

> **Note:** Hibernate auto-creates this table on startup (`ddl-auto=update`).

### Example Row

```
id            : 1
original_url  : https://google.com/search?q=very+long+query+here
short_code    : aB3xY2
custom_alias  : google-search
created_at    : 2024-01-15 10:30:00
expiry_date   : 2025-01-15 00:00:00
click_count   : 127
last_accessed : 2024-06-20 14:22:11
```

---

## API Reference

### 1. Shorten a URL

```
POST /api/shorten
Content-Type: application/json
```

**Request Body:**
```json
{
  "url": "https://www.google.com/search?q=spring+boot+tutorial",
  "customAlias": "spring-tut",
  "expiryDate": "2025-12-31T23:59:59"
}
```
> `customAlias` and `expiryDate` are **optional**.

**Response (201 Created):**
```json
{
  "shortUrl": "http://localhost:8080/spring-tut",
  "originalUrl": "https://www.google.com/search?q=spring+boot+tutorial",
  "shortCode": "spring-tut",
  "customAlias": "spring-tut",
  "createdAt": "2024-01-15T10:30:00",
  "expiryDate": "2025-12-31T23:59:59",
  "clickCount": 0,
  "lastAccessed": null
}
```

---

### 2. Redirect to Original URL

```
GET /{shortCode}
```

**Example:** `GET http://localhost:8080/aB3xY2`

**Response:** `HTTP 302 Found`
```
Location: https://www.google.com/search?q=spring+boot+tutorial
```
> The browser automatically follows this redirect.

---

### 3. Get Analytics / Stats

```
GET /api/stats/{shortCode}
```

**Response (200 OK):**
```json
{
  "shortUrl": "http://localhost:8080/aB3xY2",
  "originalUrl": "https://www.google.com/search?q=spring+boot+tutorial",
  "shortCode": "aB3xY2",
  "clickCount": 127,
  "lastAccessed": "2024-06-20T14:22:11",
  "createdAt": "2024-01-15T10:30:00",
  "expiryDate": null
}
```

---

### 4. List All URLs

```
GET /api/urls
```

**Response:** Array of `UrlResponse` objects.

---

### 5. Delete a Short URL

```
DELETE /api/urls/{shortCode}
```

**Response:** `HTTP 204 No Content`

---

### Error Responses

| HTTP Code | When |
|-----------|------|
| 400 Bad Request | Invalid URL format, validation failure |
| 404 Not Found | Short code doesn't exist |
| 409 Conflict | Custom alias already taken |
| 410 Gone | Short URL has expired |
| 500 Internal Server Error | Unexpected server error |

**Error Response Format:**
```json
{
  "timestamp": "2024-01-15T10:30:00",
  "status": 404,
  "error": "Not Found",
  "message": "Short URL 'xyz123' not found."
}
```

---

## Setup in IntelliJ IDEA

### Prerequisites — Install These First

| Tool | Download | Verify |
|------|----------|--------|
| Java 17 JDK | https://adoptium.net | `java -version` |
| Maven 3.9+ | https://maven.apache.org | `mvn -version` |
| MySQL 8.0 | https://dev.mysql.com/downloads | `mysql --version` |
| Redis | https://redis.io/download | `redis-cli ping` |
| Docker Desktop | https://www.docker.com/products/docker-desktop | `docker --version` |
| IntelliJ IDEA | https://www.jetbrains.com/idea | (community edition is free) |
| Git | https://git-scm.com | `git --version` |

---

### Step 1 — Create MySQL Database

Open MySQL Workbench or terminal:

```sql
-- Connect to MySQL
mysql -u root -p

-- Create the database
CREATE DATABASE url_shortener_db;

-- Verify it was created
SHOW DATABASES;

-- Exit
EXIT;
```

---

### Step 2 — Start Redis

**On Windows (with Redis installed):**
```bash
redis-server
```

**On Mac (with Homebrew):**
```bash
brew services start redis
```

**On Linux:**
```bash
sudo systemctl start redis
```

**Verify Redis is running:**
```bash
redis-cli ping
# Should output: PONG
```

---

### Step 3 — Open Project in IntelliJ IDEA

1. Open **IntelliJ IDEA**
2. Click **"Open"** (not "New Project")
3. Navigate to the `url-shortener` folder → click **OK**
4. IntelliJ will detect it's a Maven project
5. Click **"Load Maven Project"** in the bottom-right popup
6. Wait for Maven to download all dependencies (~2 minutes first time)

---

### Step 4 — Configure application.properties

Open `src/main/resources/application.properties` and update:

```properties
# Change these to YOUR MySQL credentials
spring.datasource.username=root
spring.datasource.password=YOUR_MYSQL_PASSWORD
```

---

### Step 5 — Enable Lombok in IntelliJ

Lombok generates code at compile time. IntelliJ needs a plugin to understand it:

1. Go to **File → Settings → Plugins**
2. Search for **"Lombok"**
3. Install it → Restart IntelliJ
4. Go to **File → Settings → Build, Execution, Deployment → Compiler → Annotation Processors**
5. Check **"Enable annotation processing"** → Apply

---

### Step 6 — Run the Application

**Option A: From IntelliJ**
1. Open `UrlShortenerApplication.java`
2. Click the green ▶ play button next to `main()`
3. Or press **Shift + F10**

**Option B: From Terminal (inside IntelliJ)**
```bash
mvn spring-boot:run
```

**Expected output:**
```
  .   ____          _            __ _ _
 /\\ / ___'_ __ _ _(_)_ __  __ _ \ \ \ \
( ( )\___ | '_ | '_| | '_ \/ _` | \ \ \ \
 \\/  ___)| |_)| | | | | || (_| |  ) ) ) )
  '  |____| .__|_| |_|_| |_\__, | / / / /
 =========|_|==============|___/=/_/_/_/
 :: Spring Boot ::                (v3.2.0)

Started UrlShortenerApplication in 3.421 seconds
Tomcat started on port(s): 8080
```

---

### Step 7 — Verify It Works

Open browser and visit:
- `http://localhost:8080/swagger-ui/index.html` → Swagger UI
- `http://localhost:8080/actuator/health` → Health check (should show `{"status":"UP"}`)

---

## Terminal Commands

### Create the Entire Project Structure from Scratch

Run these commands in your terminal (Git Bash on Windows, Terminal on Mac/Linux):

```bash
# Navigate to where you want the project
cd ~/Desktop

# Create root directory
mkdir url-shortener
cd url-shortener

# Create Maven source directories
mkdir -p src/main/java/com/project/urlshortener/controller
mkdir -p src/main/java/com/project/urlshortener/service
mkdir -p src/main/java/com/project/urlshortener/repository
mkdir -p src/main/java/com/project/urlshortener/model
mkdir -p src/main/java/com/project/urlshortener/dto
mkdir -p src/main/java/com/project/urlshortener/util
mkdir -p src/main/java/com/project/urlshortener/exception
mkdir -p src/main/java/com/project/urlshortener/config
mkdir -p src/main/resources
mkdir -p src/test/java/com/project/urlshortener

# Verify structure was created
find src -type d
```

**Expected output:**
```
src/main/java/com/project/urlshortener
src/main/java/com/project/urlshortener/controller
src/main/java/com/project/urlshortener/service
src/main/java/com/project/urlshortener/repository
src/main/java/com/project/urlshortener/model
src/main/java/com/project/urlshortener/dto
src/main/java/com/project/urlshortener/util
src/main/java/com/project/urlshortener/exception
src/main/java/com/project/urlshortener/config
src/main/resources
src/test/java/com/project/urlshortener
```

Then copy each `.java` file from this project into the corresponding folder.

---

### Git Setup

```bash
# Initialize git repository
git init

# Add all files
git add .

# First commit
git commit -m "feat: initial URL shortener implementation"

# Create GitHub repo, then:
git remote add origin https://github.com/YOUR_USERNAME/url-shortener.git
git branch -M main
git push -u origin main
```

---

## Running Without Docker

### Prerequisites Running Locally:
1. MySQL running on `localhost:3306`
2. Redis running on `localhost:6379`
3. Database `url_shortener_db` created

```bash
# Build the project
mvn clean install

# Run the application
mvn spring-boot:run

# Or run the JAR directly
java -jar target/url-shortener-1.0.0.jar
```

---

## Running With Docker

> **Easiest method — no MySQL or Redis installation needed!**

```bash
# 1. Make sure Docker Desktop is running

# 2. Build and start all 3 services (MySQL + Redis + App)
docker-compose up -d

# 3. Watch logs
docker-compose logs -f app

# 4. Check all services are running
docker-compose ps

# 5. Stop everything
docker-compose down

# 6. Stop and delete all data (volumes)
docker-compose down -v
```

**Services started:**

| Service | URL | Purpose |
|---------|-----|---------|
| Spring Boot App | http://localhost:8080 | Main application |
| MySQL | localhost:3306 | Database |
| Redis | localhost:6379 | Cache |
| Swagger UI | http://localhost:8080/swagger-ui/index.html | API Docs |

---

## Testing with Postman

### Import the collection or test manually:

**1. Shorten a URL**
```
Method: POST
URL: http://localhost:8080/api/shorten
Headers: Content-Type: application/json
Body (raw JSON):
{
  "url": "https://www.youtube.com/watch?v=very-long-video-id-here"
}
```

**2. Shorten with custom alias**
```
Method: POST
URL: http://localhost:8080/api/shorten
Body:
{
  "url": "https://chat.openai.com",
  "customAlias": "chatgpt"
}
```

**3. Redirect (paste in browser)**
```
http://localhost:8080/chatgpt
```

**4. Get stats**
```
Method: GET
URL: http://localhost:8080/api/stats/chatgpt
```

**5. Delete**
```
Method: DELETE
URL: http://localhost:8080/api/urls/chatgpt
```

---

## How Each Concept Works

### Base62 Encoding

```
Characters: a-z (26) + A-Z (26) + 0-9 (10) = 62 total

Algorithm:
  number = 12345678
  
  Step 1: 12345678 % 62 = 56 → '4'
  Step 2: 12345678 / 62 = 199123
  Step 3: 199123 % 62 = 33 → 'H'
  Step 4: 199123 / 62 = 3211
  Step 5: 3211 % 62 = 23 → 'X'
  Step 6: 3211 / 62 = 51
  Step 7: 51 % 62 = 51 → 'Z'
  
  Result: "ZXH4" (reversed) → padded to 6 chars

Capacity: 62^6 = 56,800,235,584 unique codes (~56 billion)
```

### Redis Caching Flow

```
First request for /aB3xY2:
  Browser → Spring Boot → Cache MISS → MySQL query
                        ← Store in Redis (key="urls::aB3xY2")
                        ← Return original URL → redirect

Second request for /aB3xY2:
  Browser → Spring Boot → Cache HIT → Redis read (0.1ms)
                        ← Return original URL → redirect
                        (MySQL never touched)
```

### HTTP 302 vs 301 Redirect

```
301 Permanent Redirect:
  → Browser caches it forever
  → Second visit: browser goes directly to original URL
  → Our server never sees the request → click count = WRONG ❌

302 Temporary Redirect:
  → Browser does NOT cache it
  → Every visit hits our server first
  → We can count every click → click count = CORRECT ✅
```

### Spring Layers Architecture

```
HTTP Request
     ↓
[Controller Layer]     → Handles HTTP: reads request, writes response
     ↓
[Service Layer]        → Business logic: generate code, validate, cache
     ↓
[Repository Layer]     → Database operations (Hibernate/JPA)
     ↓
[MySQL Database]       → Persistent storage
```

---

## Interview Q&A

**Q: How does URL shortening work?**
> "The system receives a long URL, generates a unique 6-character code using Base62 encoding, stores the mapping in MySQL, and returns the short URL. When the short URL is accessed, we look up the code, find the original URL, and issue an HTTP 302 redirect."

**Q: Why Base62 instead of UUID or MD5?**
> "UUID is 36 characters — too long. MD5 produces 32-character hex strings. Base62 with 6 characters gives 56 billion unique combinations, which is sufficient, and the codes are short and URL-safe (no special characters)."

**Q: Why Redis? Can't we just use MySQL for everything?**
> "MySQL reads involve disk I/O and network latency — typically 1–10ms per query. Redis is in-memory, so reads take under 0.1ms. For a URL shortener, every redirect hits the same hot URLs repeatedly. Caching in Redis means we only query MySQL once per URL — all subsequent requests are served from cache."

**Q: How do you handle race conditions in click counting?**
> "Instead of read-modify-write (which has a race condition under concurrent load), I use a single atomic SQL UPDATE: `UPDATE urls SET click_count = click_count + 1 WHERE short_code = ?`. The database handles the atomicity — no application-level locking needed."

**Q: What happens if two requests generate the same short code simultaneously?**
> "The `short_code` column has a UNIQUE constraint in MySQL. If two requests generate the same code, one INSERT will fail with a constraint violation. The service layer handles this with a retry loop — it regenerates the code and tries again, up to 5 times. With 56 billion possible codes, actual collision is extremely rare."

**Q: Why use an interface (UrlService) if there's only one implementation?**
> "Interfaces support the Dependency Inversion Principle. The controller depends on the abstraction (UrlService), not the concrete class (UrlServiceImpl). This makes the code easily testable — in unit tests, we inject a mock UrlService without needing a real database."

**Q: What is Spring's @Transactional annotation?**
> "It wraps the method in a database transaction. If any exception is thrown, all DB changes in that method are rolled back automatically. Without it, a partial failure could leave data inconsistent — for example, saving the URL but failing to update the click count."

---

## Resume Description

**URL Shortener Service** | Java, Spring Boot, MySQL, Redis, Docker

- Designed and implemented a RESTful URL shortening service using Spring Boot 3 and Hibernate ORM
- Implemented Base62 encoding algorithm to generate unique short codes (56B+ combinations)
- Integrated Redis caching to reduce database load, achieving sub-millisecond redirect resolution
- Built click analytics system tracking total redirects and last-accessed timestamps with atomic SQL updates
- Containerized the full stack (App + MySQL + Redis) using Docker Compose for reproducible deployments
- Documented all APIs using Swagger OpenAPI 3.0 and wrote unit tests using JUnit 5 and Mockito

---

*Built for learning. Every line commented. Every decision explained.*
