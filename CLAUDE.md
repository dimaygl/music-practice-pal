# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project

Practice Pal is an early-stage Spring Boot 4.1.1 / Java 21 web app (Spring MVC + Thymeleaf + Bootstrap 5 via CDN) serving a dark-themed daily guitar practice dashboard. Content is currently hardcoded sample data in the template; accounts, persistence, and dynamic content are not implemented.

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
- **No controllers exist yet.** `templates/index.html` is served at `/` by Spring Boot's Thymeleaf welcome-page auto-configuration. The template links to `/login` and `/song-charts`, which have no handlers yet — adding those pages means adding `@Controller`s and templates.
- **Security:** `spring-boot-starter-security` is on the classpath, and `SecurityConfig` defines a `SecurityFilterChain` that permits all requests (no form login configured). Any new auth work should modify this bean rather than relying on Boot's default security. `thymeleaf-extras-springsecurity6` is available for `sec:` attributes in templates.
- **Data:** H2 (plus `spring-boot-h2console`) and PostgreSQL drivers are runtime dependencies, but no JPA/JDBC starter, datasource settings, or entities exist yet. `application.properties` only sets `spring.application.name`.
- Lombok is configured as an annotation processor for both main and test compilation.
- Actuator is included with default exposure.
- Tests use Spring Boot 4's modular test starters (`spring-boot-starter-webmvc-test`, `-security-test`, `-thymeleaf-test`, `-actuator-test`); currently only a `@SpringBootTest` context-load test exists.
