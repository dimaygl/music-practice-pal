# Practice Pal

**A simple home for your daily guitar practice.**

Practice Pal is a Spring Boot web application for keeping guitar practice focused. Its home page brings together a daily scale, song, and lesson in one place, with a separate entry point for song charts.

> **Project status:** The dashboard selects a daily song, lesson, scale, and minor backing track from local data. Song links are stored in H2; an in-memory administrator account protects song-link editing and chart uploads. Saved practice plans and persistent user accounts are not implemented.

## What’s included

- A responsive dashboard with daily practice sections for scales, songs, and lessons
- A song chart gallery with a grid and image-preview modals
- An administrator login modal for protected song-chart uploads
- Persistent song links with an administrator form to add links
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
│       ├── data.json                  # Song list and ordered scale/minor/backing-track data
│       └── application.properties     # Application configuration
└── test/                              # Application tests
```

## Configuration

`src/main/resources/data.json` contains `Songs` and `Lessons` arrays and a `Scales` object. Each song has a `name` and optional `chartImage`; song links are stored in the H2 database using Spring Data JPA and displayed in the dashboard. Administrators can add HTTP(S) links for the current practice song from its dashboard card. Default song links are seeded idempotently at application startup. Each lesson has a `name` and a `links` array of URLs; links for the selected daily song and lesson appear in their dashboard panels. The song list, lesson list, and scale entries each rotate in their listed order, advancing once per calendar day from Monday, January 5, 1970, and wrapping back to the first entry. Each scale key maps to an object containing a `relativeMinor` label and `backingTrackUrl`.

Use the Song Charts grid's upload form to add a PNG, JPG, JPEG, or GIF chart image to a song. Uploaded images are stored in `uploads/song-charts` by default and served from the gallery; change `practice-pal.upload-dir` to choose another storage directory. Upload size is limited to 10 MB. The gallery opens a chart image in a modal, and songs without a chart show a placeholder.

The application name and file-backed H2 connection are set in `src/main/resources/application.properties`. Database files are stored under `data/` by default and retained across restarts. Spring Boot uses its standard configuration conventions, so the datasource URL and other settings can be supplied with environment variables or command-line properties.

The default administrator login is `admin` / `password`. Configure `SPRING_SECURITY_USER_NAME` and `SPRING_SECURITY_USER_PASSWORD` to set different credentials; change the default password before exposing the app outside a trusted local environment. The account is kept in memory and has to be configured again after each restart.

H2 and PostgreSQL drivers are included as runtime dependencies. The app uses H2 for persistent song links; other application data and user accounts are not stored in the database.

## Contributing

Contributions and ideas are welcome. For substantial changes, open an issue first to discuss the proposed direction.
