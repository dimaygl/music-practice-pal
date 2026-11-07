# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project

Practice Pal is a Spring Boot 4.1.1 / Java 21 web app (Spring MVC + Thymeleaf + Bootstrap 5 via CDN) serving a dark-themed daily guitar practice dashboard. Songs, lessons, and song links use Spring Data JPA with file-backed H2 by default; the administrator account remains in memory.

## Commands

There is no Maven wrapper — use a system `mvn` (3.6.3+) with JDK 21+.

```bash
mvn spring-boot:run                       # run at http://localhost:8080 (devtools enabled)
mvn test                                  # all tests
mvn test -Dtest=PracticePalApplicationTests            # single test class
mvn test -Dtest=PracticePalApplicationTests#contextLoads  # single test method
mvn package                               # builds target/music-practice-pal.jar
```

Note: `pom.xml` sets `<finalName>music-practice-pal</finalName>`, so the jar is `target/music-practice-pal.jar` (the README's `-0.0.1-SNAPSHOT` path is outdated).

## Architecture

- Base package `musicpracticepal`; entry point `PracticePalApplication`, config classes under `musicpracticepal`.
- `MainController` loads songs and lessons from JPA repositories and rotates them daily by their persisted `position`. `src/main/resources/data.sql` idempotently seeds the initial catalog after Hibernate initializes the schema.
- **Security:** `spring-boot-starter-security` is on the classpath, and `SecurityConfig` defines a `SecurityFilterChain` that permits all requests (no form login configured). Any new auth work should modify this bean rather than relying on Boot's default security. `thymeleaf-extras-springsecurity6` is available for `sec:` attributes in templates.
- **Data:** `Song`, `SongLink`, and `Lesson` are JPA entities. H2 (plus `spring-boot-h2console`) and PostgreSQL drivers are runtime dependencies; the default H2 database is file-backed under `data/`.
- Lombok is configured as an annotation processor for both main and test compilation.
- Actuator is included with default exposure.
- Tests use Spring Boot 4's modular test starters (`spring-boot-starter-webmvc-test`, `-security-test`, `-thymeleaf-test`, `-actuator-test`) and cover JPA seeding and application behavior.
