# Practice Pal

**A simple home for your daily guitar practice.**

Practice Pal is a Spring Boot web application for keeping guitar practice focused. Its home page brings together a daily scale, song, and lesson in one place, with a separate entry point for song charts.

> **Project status:** The dashboard selects a scale and minor backing track from local data. Song and lesson content is still sample content; user accounts, saved practice plans, and persistence are not implemented.

## What’s included

- A responsive dashboard with daily practice sections for scales, songs, and lessons
- A navigation entry for song charts
- A dark-themed interface built with Thymeleaf and Bootstrap
- A Spring Boot application configured for Java 21

## Tech stack

| Area | Technology |
| --- | --- |
| Language | Java 21 |
| Application | Spring Boot 4.1.1 |
| Web UI | Spring MVC, Thymeleaf, Bootstrap 5 |
| Build | Maven |
| Database drivers | H2 and PostgreSQL (runtime dependencies) |

## Get started

### Prerequisites

- Java 21 or newer
- Maven 3.6.3 or newer

### Run locally

Clone the repository, then start the application:

```bash
git clone https://github.com/dimaygl/music-practice-pal.git
cd music-practice-pal
mvn spring-boot:run
```

Open [http://localhost:8080](http://localhost:8080) in your browser.

### Build and test

```bash
mvn test
mvn package
```

The packaged application can be started with:

```bash
java -jar target/music-practice-pal-0.0.1-SNAPSHOT.jar
```

## Project layout

```text
src/
├── main/
│   ├── java/org/practicepal/          # Spring Boot application and configuration
│   └── resources/
│       ├── templates/                 # Thymeleaf pages
│       ├── data.json                  # Ordered scale, relative-minor, and backing-track data
│       └── application.properties     # Application configuration
└── test/                              # Application tests
```

## Configuration

`src/main/resources/data.json` defines the scales, their relative minor names, and backing-track URLs. Each top-level key is a scale, and its object contains a `relativeMinor` label and `backingTrackUrl`. The JSON entry order determines the daily scale sequence; it advances once per calendar day from Monday, January 5, 1970, and wraps back to the first entry.

The application name is set in `src/main/resources/application.properties`. Spring Boot uses its standard configuration conventions, so additional settings can be supplied with environment variables or command-line properties.

H2 and PostgreSQL are included as runtime dependencies. Database connection settings and application data persistence are not configured yet.

## Contributing

Contributions and ideas are welcome. For substantial changes, open an issue first to discuss the proposed direction.
