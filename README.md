# Practice Pal

**A simple home for your daily guitar practice.**

Practice Pal is a Spring Boot web application for keeping guitar practice focused. Its home page brings together a daily scale, song, and lesson in one place, with a separate Songs page for the song catalog and charts.

> **Project status:** The dashboard selects a daily song, lesson, scale, and minor backing track. Songs, lessons, song links, and chart images are stored in an in-memory H2 database by default and in PostgreSQL under the production profile. An in-memory administrator account protects catalog management, link editing, and chart uploads. Saved practice plans and persistent user accounts are not implemented.

## What’s included

- A responsive dashboard with daily practice sections for scales, songs, and lessons
- A song chart gallery with a grid and image-preview modals
- An administrator login modal for protected song-chart uploads
- Persistent songs, lessons, and song links stored through Spring Data JPA
- Administrator controls to add/remove songs and lessons, manage song links, and update a lesson's single optional link
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

Open [http://localhost:8082](http://localhost:8082) in your browser.

### Build and test

```bash
mvn test
mvn package
```

The packaged application can be started with:

```bash
java -jar target/music-practice-pal-0.0.1-SNAPSHOT.jar
```

The packaged jar is named `target/music-practice-pal.jar`.

## Project layout

```text
src/
├── main/
│   ├── java/org/musicpracticepal/     # Spring Boot application and configuration
│   └── resources/
│       ├── templates/                 # Thymeleaf pages
│       └── application.properties     # Application configuration
└── test/                              # Application tests
```

## Configuration

`src/main/resources/schema.sql` renames an existing `practice_song` table to `song` on startup, preserving its rows. `src/main/resources/data.sql` seeds the initial song and lesson when records are missing; it runs after Hibernate creates or updates the JPA schema. The Lessons page lists lessons and lets administrators add or remove them; the last lesson cannot be removed. Each lesson has one optional HTTP(S) link stored directly on its row; administrators can update or clear it from the dashboard. Existing links are migrated from `lesson_link` to the lesson row (keeping the first link), then the old table is removed. Administrators can also add/remove songs and add HTTP(S) links for a song from its dashboard card; the last song cannot be removed. The song and lesson lists, as well as scale entries, rotate in their listed order, advancing once per calendar day from Monday, January 5, 1970, and wrapping back to the first entry. Scale and chord progression details are defined in the application code.

Use the Songs page grid's upload form to add a PNG, JPG, JPEG, or GIF chart image to a song. Chart images are stored in the selected database and served from the gallery. Upload size is limited to 10 MB. The gallery opens a chart image in a modal, and songs without a chart show a placeholder.

### Piano audio credits

The chord-progression piano samples are curated and edited by Nicholaus P. Brosowsky for [tonejs-instruments](https://github.com/nbrosowsky/tonejs-instruments) and originate from the [Versilian Studios Chamber Orchestra Community (VSO2)](http://vis.versilstudios.net/vsco-community.html). They are licensed under [Creative Commons Attribution 3.0 (CC BY 3.0)](https://creativecommons.org/licenses/by/3.0/).

The application name and in-memory H2 connection are set in `src/main/resources/application.properties`; local data is reset when the application stops. To use PostgreSQL, activate the `production` profile; it uses `DB_URL`, `DB_USERNAME`, and `DB_PASSWORD` (defaulting to the local `music-practice-pal` database). On startup, the app creates the database named in `DB_URL` if it is missing. The configured PostgreSQL user must be able to connect to the `postgres` maintenance database and have the `CREATEDB` privilege.

The default administrator login is `admin` / `password`. Configure `SPRING_SECURITY_USER_NAME` and `SPRING_SECURITY_USER_PASSWORD` to set different credentials; change the default password before exposing the app outside a trusted local environment. The account is kept in memory and has to be configured again after each restart.

H2 and PostgreSQL drivers are included as runtime dependencies. Songs, lessons, song links, and chart images use the selected database; user accounts are not stored in the database.

## Contributing

Contributions and ideas are welcome. For substantial changes, open an issue first to discuss the proposed direction.
