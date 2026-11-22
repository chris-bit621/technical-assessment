# EatClub Technical Solution

Java 21 Spring Boot implementation of the EatClub take-home. Open this folder's `pom.xml` as a Maven project in IntelliJ.

## Run locally

macOS and Linux

```bash
chmod +x ./mvnw
./mvnw spring-boot:run
```

Windows PowerShell

```powershell
.\mvnw.cmd spring-boot:run
```

The app listens on port 8080. It exposes `GET /deals?timeOfDay=15:00` and `GET /deals/peak`.

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

## Live feed verification

I ran the application against the EatClub feed and checked these requests on 22 November 2026.

| Request | Result |
| --- | --- |
| `GET /deals?timeOfDay=3:00pm` | 5 deals |
| `GET /deals?timeOfDay=6:00pm` | 9 deals |
| `GET /deals?timeOfDay=9:00pm` | 4 deals |
| `GET /deals/peak` | `18:00–20:59` inclusive |

The peak ends at `20:59` because that is the last minute at the maximum count. The count drops at `21:00`.

## Task 3

[MongoDB schema design](TASK-3.md)

## Tests

macOS and Linux

```bash
./mvnw test
```

Windows PowerShell

```powershell
.\mvnw.cmd test
```
