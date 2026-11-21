# EatClub Technical Solution

Java 21 Maven starter for implementing the EatClub take-home. Open this folder's `pom.xml` as a Maven project in IntelliJ.

## Run locally

```powershell
.\mvnw.cmd spring-boot:run
```

The app listens on port 8080. The starter has the synchronous HTTP client and the source and API records. The controller exposes `GET /deals?timeOfDay=15:00`. The service currently returns an empty deals array as a placeholder.

## Task 1

- Accepts both 24 hr format and case-insensitive AM/PM format in parameter of `timeOfDay`.
- A deal is active when the restaurant is open and the deal's own window, if present, is active.
- Prefer a complete deal `open`/`close` pair, then a complete `start`/`end` pair, then inherit restaurant hours.
- Include the start and exclude the end. Cover overnight windows. Keep `restarantSuburb` and the string-valued deal fields in the response. Count entries independently of quantity and duplicate IDs.
- Return HTTP 400 for missing or invalid input and HTTP 502 for upstream failure.

## Tests

```powershell
.\mvnw.cmd test
```

The Task 1 tests use synthetic restaurants and a stubbed feed client. Several tests are expected to fail until the exercise is implemented. To verify that the starter compiles and packages before implementing the exercise, run `.\mvnw.cmd -DskipTests package`.
