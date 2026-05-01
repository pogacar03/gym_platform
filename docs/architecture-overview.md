# FitMate Architecture Overview

FitMate is a Java-based personalized workout recommendation platform. Users maintain a training profile, describe their exercise needs in natural language, receive safe video-based workout recommendations, and record completion feedback. Administrators maintain the workout catalog, import YouTube channel content, review suggested tags, and keep the recommendation library clean.

## 1. Technology Stack

| Layer | Technology | Responsibility |
| --- | --- | --- |
| Frontend rendering | Thymeleaf, Bootstrap, vanilla JavaScript | Server-rendered pages, forms, language switching, lightweight UI interactions |
| Web framework | Spring Boot 3 | MVC routing, dependency injection, configuration, application lifecycle |
| Security | Spring Security | Login, remember-me token, role-based access control |
| Data access | MyBatis-Plus, MyBatis XML | CRUD operations and custom SQL queries |
| Database | PostgreSQL 16 | Users, profiles, videos, plans, logs, imports, recommendation history |
| Migration | Flyway | Versioned schema creation and seed data |
| Search | Elasticsearch 8 | Keyword and semantic-style workout video retrieval |
| LLM integration boundary | LangChain4j dependency + `LlmGateway` abstraction | Recommendation explanation generation, replaceable provider boundary |
| Local AI support | `TemplateLlmGateway`, `EmbeddingService`, knowledge JSON | Offline explanation fallback, local embedding vector generation, training knowledge notes |
| Deployment | Docker Compose + Maven | Local PostgreSQL, Elasticsearch, Spring Boot runtime |

## 2. High-Level Architecture

```mermaid
flowchart LR
    User["User Browser"] --> Thymeleaf["Thymeleaf Pages"]
    Admin["Admin Browser"] --> Thymeleaf

    Thymeleaf --> Controllers["Spring MVC Controllers"]
    Controllers --> Services["Application Services"]
    Services --> Mappers["MyBatis-Plus Mappers"]
    Mappers --> PostgreSQL[("PostgreSQL")]

    Services --> SearchService["WorkoutVideoSearchService"]
    SearchService --> Elasticsearch[("Elasticsearch Index")]

    Services --> LlmGateway["LlmGateway"]
    LlmGateway --> TemplateGateway["TemplateLlmGateway / LangChain4j Boundary"]

    Services --> Knowledge["Knowledge JSON Files"]
    Services --> ImportPipeline["Video Import Pipeline"]
    ImportPipeline --> YouTube["YouTube RSS Feeds"]
    ImportPipeline --> PostgreSQL
    ImportPipeline --> Elasticsearch
```

The system is intentionally designed as a layered monolith. This keeps the graduation-scale implementation understandable while still preserving clear module boundaries: web controllers do not directly query the database, recommendation logic is isolated in services, and search/LLM capabilities are wrapped behind replaceable interfaces.

## 3. Package Structure

```mermaid
flowchart TB
    Root["com.graduation.fitmate"]
    Root --> Config["config<br/>Security, LLM, search properties"]
    Root --> Controller["controller<br/>MVC route handlers"]
    Root --> Service["service<br/>Business logic"]
    Root --> Mapper["mapper<br/>MyBatis-Plus database access"]
    Root --> Entity["entity<br/>Database table models"]
    Root --> DTO["dto<br/>Form objects and view models"]
    Root --> LLM["llm<br/>LLM gateway abstraction"]
    Root --> Search["search<br/>Elasticsearch document model"]
    Root --> Util["util<br/>UI display helper"]
```

## 4. MVC Request Flow

```mermaid
sequenceDiagram
    participant Browser
    participant Controller
    participant Service
    participant Mapper
    participant DB as PostgreSQL
    participant View as Thymeleaf

    Browser->>Controller: HTTP request
    Controller->>Service: Validate input and call business method
    Service->>Mapper: Query or persist data
    Mapper->>DB: SQL
    DB-->>Mapper: Rows
    Mapper-->>Service: Entities
    Service-->>Controller: DTO / View model
    Controller-->>View: Model attributes
    View-->>Browser: Rendered HTML
```

Example pages following this pattern:

- `HomeController` renders dashboard and records latest workout feedback.
- `ProfileController` manages user training profile.
- `RecommendationController` receives structured quick filters plus natural language request.
- `WorkoutVideoController` lists and displays workout videos.
- `AdminVideoController` and `AdminImportController` manage catalog operations.

## 5. Authentication And Authorization

```mermaid
flowchart TD
    Start["User opens protected page"] --> AuthCheck{"Authenticated?"}
    AuthCheck -- No --> Login["Redirect to /login"]
    Login --> Credential["Submit username/password"]
    Credential --> Provider["DaoAuthenticationProvider"]
    Provider --> UserDetails["CustomUserDetailsService"]
    UserDetails --> Account[("user_account")]
    Provider --> Success{"Valid credentials?"}
    Success -- No --> LoginError["Show login error"]
    Success -- Yes --> Session["Create session / remember-me token"]
    Session --> RoleCheck{"Accessing /admin/**?"}
    AuthCheck -- Yes --> RoleCheck
    RoleCheck -- No --> UserPage["User pages"]
    RoleCheck -- Yes --> AdminRole{"ROLE_ADMIN?"}
    AdminRole -- Yes --> AdminPage["Admin pages"]
    AdminRole -- No --> Denied["403 Forbidden"]
```

Security rules:

- Public: `/`, `/login`, `/register`, `/css/**`
- Authenticated user: dashboard, profile, videos, recommendations
- Admin only: `/admin/**`
- Remember-me token storage: `persistent_logins`
- UI role separation: admin navigation entries are rendered only for `ROLE_ADMIN`

## 6. Recommendation Pipeline

```mermaid
flowchart TD
    A["User submits request<br/>quick filters + natural language"] --> B["RecommendationController"]
    B --> C["RequestParsingService<br/>extract duration, posture, equipment, goal, body area, safety constraints"]
    C --> D["Load user profile"]
    D --> E["Apply recent feedback adjustment<br/>too easy / just right / too hard"]
    E --> F["Safety and rule filters<br/>avoid incompatible equipment, high impact, sensitive areas"]
    F --> G["WorkoutVideoSearchService"]
    G --> H["Elasticsearch keyword search"]
    G --> I["Elasticsearch vector-style search"]
    H --> J["Merge and normalize candidate scores"]
    I --> J
    J --> K["Fetch candidate videos from PostgreSQL"]
    K --> L["Business ranking<br/>goal, duration, posture, equipment, impact"]
    L --> M["Fallback path if no suitable match"]
    M --> N["Build WorkoutPlan and WorkoutPlanItem"]
    L --> N
    N --> O["RecommendationKnowledgeService<br/>training notes"]
    O --> P["LlmGateway<br/>generate explanation text"]
    P --> Q["RecommendationResult"]
    Q --> R["Thymeleaf recommendation page"]
```

Important design point: the system does not let a large language model directly choose arbitrary videos. The recommendation process is controlled by structured parsing, rule filtering, SQL/Elasticsearch retrieval, deterministic ranking, and only then explanation generation. This makes the result easier to justify and safer to demonstrate.

## 7. Search And Ranking Design

```mermaid
flowchart LR
    Request["ParsedRecommendationRequest"] --> QueryBuilder["Search query builder"]
    QueryBuilder --> Lexical["Keyword fields<br/>title, description, tags, channel"]
    QueryBuilder --> Vector["Embedding vector<br/>local hashed token vector"]
    Lexical --> ES[("Elasticsearch")]
    Vector --> ES
    ES --> Scores["SearchCandidateScore<br/>lexical score + vector score"]
    Scores --> Merge["Weighted merge<br/>lexicalWeight + vectorWeight"]
    Merge --> CandidateIds["Candidate video ids"]
    CandidateIds --> DB[("PostgreSQL workout_video")]
    DB --> BusinessScore["Business fit scoring"]
    BusinessScore --> FinalList["Final recommended videos"]
```

Current search behavior:

- Keyword matching handles direct user intent such as "chair workout", "low impact", "back".
- Vector-style matching improves recall for semantically related text.
- Candidate scores are exposed in the recommendation result as ranking explanations.
- If Elasticsearch fails or returns no safe candidate, the system falls back to database retrieval.

## 8. Content Import Pipeline

```mermaid
sequenceDiagram
    participant Admin
    participant ImportController
    participant ImportService
    participant YouTube as YouTube RSS
    participant Tagger as ImportedVideoTaggingService
    participant DB as PostgreSQL
    participant Index as Elasticsearch Index

    Admin->>ImportController: Add source / run import
    ImportController->>ImportService: importFromSource(sourceId)
    ImportService->>YouTube: Fetch channel feed
    YouTube-->>ImportService: Video metadata
    ImportService->>Tagger: Infer goal, body area, equipment, posture, impact, extra tags
    Tagger-->>ImportService: Suggested tags + confidence
    ImportService->>DB: Upsert imported_video
    Admin->>ImportController: Approve staged video
    ImportController->>ImportService: approveImportedVideo()
    ImportService->>DB: Upsert workout_video
    ImportService->>Index: Sync searchable document
```

This pipeline reduces manual content entry. The administrator still controls final approval, but source metadata, thumbnails, inferred tags and duplicate handling are automated.

## 9. Feedback Loop

```mermaid
flowchart TD
    A["User completes latest plan"] --> B["workout_log"]
    B --> C["User selects feedback<br/>Too easy / Just right / Too hard"]
    C --> D["DashboardService records feedback"]
    D --> E["RecommendationService reads latest feedback"]
    E --> F{"Feedback type"}
    F -- "Too hard" --> G["Reduce duration, prefer low impact / beginner / recovery"]
    F -- "Too easy" --> H["Increase duration, allow more moderate intensity"]
    F -- "Just right" --> I["Keep similar training direction"]
    G --> J["Next recommendation"]
    H --> J
    I --> J
```

The feedback loop is simple but useful for product completeness: the dashboard is not only a display page; it changes the next recommendation behavior.

## 10. Database ER Diagram

```mermaid
erDiagram
    USER_ACCOUNT ||--o| USER_PROFILE : owns
    USER_ACCOUNT ||--o{ RECOMMENDATION_HISTORY : creates
    USER_ACCOUNT ||--o{ WORKOUT_PLAN : owns
    USER_ACCOUNT ||--o{ WORKOUT_LOG : records

    WORKOUT_PLAN ||--o{ WORKOUT_PLAN_ITEM : contains
    WORKOUT_VIDEO ||--o{ WORKOUT_PLAN_ITEM : referenced_by
    WORKOUT_PLAN ||--o{ WORKOUT_LOG : completed_as

    WORKOUT_VIDEO ||--o{ WORKOUT_VIDEO_TAG : has
    WORKOUT_TAG ||--o{ WORKOUT_VIDEO_TAG : labels

    IMPORT_SOURCE ||--o{ IMPORTED_VIDEO : provides
    IMPORTED_VIDEO }o--|| WORKOUT_VIDEO : publishes_to

    USER_ACCOUNT {
        bigint id PK
        string username
        string password_hash
        string role
        string display_name
        boolean enabled
    }

    USER_PROFILE {
        bigint id PK
        bigint user_id FK
        int age
        string gender
        string fitness_goal
        string activity_level
        string available_equipment
        string target_areas
        string injury_notes
        string posture_preference
    }

    WORKOUT_VIDEO {
        bigint id PK
        string title
        string description
        string target_goal
        string target_body_part
        string equipment_requirement
        string difficulty
        string source_type
        string video_url
        string thumbnail_url
        string posture_type
        string impact_level
        string extra_tags
        boolean active
    }

    RECOMMENDATION_HISTORY {
        bigint id PK
        bigint user_id FK
        string request_text
        string result_summary
        timestamp created_at
    }

    WORKOUT_PLAN {
        bigint id PK
        bigint user_id FK
        string title
        string summary
        timestamp created_at
    }

    WORKOUT_PLAN_ITEM {
        bigint id PK
        bigint plan_id FK
        bigint video_id FK
        int sort_order
        int duration_minutes
    }

    WORKOUT_LOG {
        bigint id PK
        bigint user_id FK
        bigint plan_id FK
        timestamp completed_at
        int duration_minutes
        int fatigue_level
        string feedback_note
    }

    IMPORT_SOURCE {
        bigint id PK
        string name
        string source_type
        string external_id
        string channel_name
        boolean enabled
    }

    IMPORTED_VIDEO {
        bigint id PK
        bigint source_id FK
        string external_video_id
        string title
        string video_url
        string thumbnail_url
        string import_status
        string suggested_extra_tags
        decimal confidence_score
    }
```

## 11. Deployment Diagram

```mermaid
flowchart TB
    subgraph LocalMachine["Local / Single Server"]
        Browser["Browser"]
        App["Spring Boot App<br/>localhost:8080"]
        Postgres["PostgreSQL 16<br/>Docker: fitmate-postgres<br/>localhost:5432"]
        ES["Elasticsearch 8<br/>Docker: fitmate-elasticsearch<br/>localhost:9200"]
        Maven["Maven Runtime"]
    end

    Browser --> App
    Maven --> App
    App --> Postgres
    App --> ES
    App --> YouTube["YouTube RSS Feeds"]
    App -. optional .-> LLM["External LLM Provider<br/>via LangChain4j boundary"]
```

Development startup:

1. `docker compose up -d`
2. `mvn spring-boot:run`
3. Open `http://localhost:8080`

## 12. Main Runtime Components

| Component | Main classes | Purpose |
| --- | --- | --- |
| Authentication | `SecurityConfig`, `CustomUserDetailsService`, `RegistrationService` | Login, roles, remember-me, registration |
| Dashboard | `HomeController`, `DashboardService`, `DashboardView` | Progress rings, weekly rhythm, recent logs, feedback |
| Profile | `ProfileController`, `UserProfileService`, `UserProfile` | Training profile and safety constraints |
| Video library | `WorkoutVideoController`, `WorkoutVideoService`, `WorkoutVideo` | Public catalog browsing and detail pages |
| Recommendation | `RecommendationController`, `RecommendationService`, `RequestParsingService` | Parse request, filter, retrieve, rank, save plan |
| Search | `WorkoutVideoSearchService`, `ElasticsearchWorkoutVideoSearchGateway`, `EmbeddingService` | Hybrid recall and score explanation |
| Knowledge notes | `RecommendationKnowledgeService`, `RecommendationKnowledgeNote` | Static training guidance selected by request/profile context |
| LLM boundary | `LlmGateway`, `TemplateLlmGateway` | Explanation generation abstraction |
| Import operations | `AdminImportController`, `VideoImportService`, `ImportedVideoTaggingService` | YouTube feed import, suggested tags, approval |
| Admin catalog | `AdminVideoController`, `WorkoutVideoService` | Manual video editing and batch tag cleanup |

## 13. Why This Architecture Is Suitable

The architecture emphasizes reliability and explainability over a fully autonomous AI agent. Exercise recommendation is a safety-sensitive domain, so FitMate uses deterministic filtering and ranking before generating user-facing explanations. Elasticsearch improves retrieval quality, PostgreSQL remains the source of truth, and the LLM integration is isolated behind a gateway so the system can still return recommendations when the LLM provider is unavailable.

Key strengths:

- Clear separation between user-facing training flow and admin content operations.
- Role-based access control protects catalog management functions.
- Hybrid search improves recall while preserving SQL fallback.
- Recommendation history, workout plans and logs create a closed user loop.
- Content import pipeline makes the video library maintainable at scale.
- Flyway migrations make the schema reproducible across machines.

