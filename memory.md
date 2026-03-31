# Project Memory

## Current Decisions
- Stack: Spring Boot 3, Thymeleaf, HTMX, MyBatis-Plus, PostgreSQL, Flyway, Spring Security, LangChain4j
- Primary delivery goal: stable defense-ready demo with end-to-end flow
- Recommendation architecture: rule filtering + SQL retrieval + LLM explanation generation
- Video handling: metadata plus external or local video URLs, no streaming pipeline
- Data access: MyBatis-Plus for CRUD, XML for non-trivial filtering queries

## Why This Direction
- Keeps the stack aligned with existing Java and MyBatis experience
- Avoids risky full-AI decision making in first milestone
- Preserves room for future upgrades such as embeddings or better ranking

## Known Risks
- Local machine currently has no Maven installed
- External LLM provider credentials are not configured yet
- PostgreSQL must be started manually or via Docker Compose

## Immediate Next Steps
1. Install Maven or add Maven wrapper from a machine with Maven.
2. Start PostgreSQL using Docker Compose.
3. Verify Flyway migration and seed data.
4. Configure `LLM_API_KEY` only after baseline flow works.

## Prompt Design Notes
- Keep structured extraction conservative.
- Prefer recommending safe alternatives over aggressive plans.
- Explain recommendation reasons in plain language suitable for student demo.

