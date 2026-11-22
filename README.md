# EatClub Technical Solution

Java 21 Maven starter for implementing the EatClub take-home. Open this folder's `pom.xml` as a Maven project in IntelliJ.

## Run locally

```powershell
.\mvnw.cmd spring-boot:run
```

The app listens on port 8080. The starter has the synchronous HTTP client and the source and API records. The controller exposes `GET /deals?timeOfDay=15:00`. The service currently returns an empty deals array as a placeholder.

## Task 1

- Accepts both 24 hr format and case-insensitive AM/PM format in parameter of `timeOfDay`.
- A deal is active when the restaurant is open, the deal's own window (if present) is active, and its `qtyLeft` is greater than zero.
- If a deal has `start/end` fields then they will be treated as the deals timings.
- If a deal has 0 quantity, it will not be treated as an active deal.
- Include the start and exclude the end. Cover overnight windows. 
- Count each active entry once, even when entries share a deal ID.
- Return HTTP 400 for missing or invalid input and HTTP 502 for upstream failure.


## Task 2
- Returns the start and end of the peak period (inclusive of both)
- Returns the earliest start peak of the window.
- A peak that continues through midnight is one window, e.g 23:00 - 01:59
- A deal follows same availability rules as in task 1.


## Tests

```powershell
.\mvnw.cmd test
```