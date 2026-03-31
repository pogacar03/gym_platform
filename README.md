# FitMate

FitMate is a Java-based graduation project for an intelligent exercise recommendation platform. It helps users describe their needs in natural language, filters a curated workout video library with safety rules, and recommends suitable YouTube-based exercise content with explanation and follow-up records.

## Project Positioning

This project is designed around a practical thesis goal:

- collect exercise videos from external platforms such as YouTube
- add structured tags for posture, equipment, difficulty, goal, and body area
- let users describe needs such as "I have no equipment and want chair exercises"
- recommend safe, relevant, and easy-to-follow exercise videos
- add AI assistance for parsing, explanation, and future auto-tagging workflows

## Current Features

### User side
- Registration and login
- Persistent login with remember-me support
- Guided profile setup with body-area selection
- Natural-language workout request form
- Rule-based recommendation results with thumbnails and source links
- Workout video library browsing and preview pages

### Admin side
- Video library management
- Import source management for YouTube channels
- Scheduled and manual import pipeline
- Auto-tagging suggestions with confidence score
- Lightweight review flow to approve or reject imported videos

### AI and recommendation
- Natural-language request parsing
- Safety filtering for knee and back sensitivity
- SQL-based candidate selection
- Template-based recommendation explanation
- Extensible LLM gateway design for LangChain4j integration

## Tech Stack

- `Java 21`
- `Spring Boot 3`
- `Thymeleaf + HTMX + Bootstrap`
- `MyBatis-Plus + MyBatis XML`
- `PostgreSQL`
- `Flyway`
- `Spring Security`
- `LangChain4j`
- `Docker Compose`

## Project Structure

```text
src/main/java/com/graduation/fitmate
├── config
├── controller
├── dto
├── entity
├── llm
├── mapper
└── service

src/main/resources
├── db/migration
├── mapper
├── static
└── templates
```

## Local Run

### 1. Start PostgreSQL

```bash
docker compose up -d
```

### 2. Start the application

```bash
mvn spring-boot:run
```

### 3. Open the app

[http://localhost:8080](http://localhost:8080)

## Demo Accounts

- `student / password123`
- `admin / password123`
- `stu / 123`

## Configuration

Important environment variables:

- `DB_URL`
- `DB_USERNAME`
- `DB_PASSWORD`
- `LLM_ENABLED`
- `LLM_API_KEY`
- `LLM_BASE_URL`
- `LLM_MODEL`

## Development Notes

- The current recommendation flow can work without an external LLM because it falls back to a template-based explanation provider.
- Video sources are currently modeled as curated YouTube content rather than downloaded local files.
- The import pipeline is designed as "auto-import + auto-tag + lightweight human review" to reduce manual curation cost.

## Validation

Run the test suite with:

```bash
mvn test
```

## Roadmap

- richer muscle-zone interaction and finer body segmentation
- editable review form before imported videos are approved
- CSV import for curated video datasets
- full LangChain4j provider integration
- recommendation history and personalized progress insights
