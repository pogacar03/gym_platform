# FitMate Midterm Demo Checklist

This checklist is for a short advisor-facing demo. The goal is to show that FitMate is already a working Java system, not only a UI mockup.

## Demo Goal

Show one complete loop:

1. A user has a training profile.
2. The user describes a real workout need.
3. FitMate parses the need into structured constraints.
4. The system retrieves and filters videos with safety rules.
5. The result explains why the videos were selected.
6. The generated plan can be saved, completed, and used as feedback for later recommendations.

## Before The Meeting

- Run `mvn test` and confirm all tests pass.
- Run `docker compose up -d` and confirm PostgreSQL and Elasticsearch are both `Up`.
- Start the app with `mvn spring-boot:run`.
- Open `http://localhost:8080/login`.
- Log in once with `student / password123` and once with `admin / password123` to confirm both accounts work.
- Keep this repository clean: recovery files such as `rollout-*.jsonl*` are local context only and should stay uncommitted.

## Recommended Demo Script

### 1. Positioning

FitMate is a Java-based intelligent fitness recommendation platform. It uses a curated workout video library, user profiles, safety constraints, hybrid retrieval, and explanation generation to recommend safer follow-along workouts.

Key thesis value:

- It is not a generic chatbot.
- It turns natural-language needs into structured recommendation conditions.
- It keeps safety filtering before ranking.
- It records recommendation evidence and user feedback for later personalization.

### 2. User Profile

Open the profile page and show:

- training goal
- preferred duration
- available equipment
- target body area
- safety notes such as knee, back, or shoulder sensitivity

Advisor talking point:

The user profile is the stable personalization layer. It reduces repeated user input and gives the recommendation service default constraints.

### 3. Recommendation Scenario

Use one of these stable demo inputs:

```text
我膝盖不太舒服，想做20分钟无器械低冲击减脂训练
```

```text
我有一点肩部不适，想做15分钟温和的肩部恢复和活动度训练
```

```text
给老人推荐一个10分钟温和的坐姿训练
```

Show the result page and point out:

- selected videos
- safety notes
- fit reasons
- filtered-out reasons
- knowledge references
- retrieval summary
- saved plan id or plan link

Advisor talking point:

The system uses rule-based safety constraints and retrieval evidence before generating user-facing explanations. This makes the result easier to defend in a thesis than a pure LLM answer.

### 4. Training Plan And Feedback

Open the saved plan or dashboard and show:

- generated plan
- plan completion action
- feedback options: too easy, just right, too hard
- dashboard summary

Advisor talking point:

Feedback creates a closed loop. Later recommendations can adjust intensity based on recent user feedback.

### 5. Admin Side

Log in as admin and show:

- video library management
- import source management
- imported video review or tagging suggestions
- knowledge base page

Advisor talking point:

The admin side solves the content quality problem. The system does not depend on random internet results during recommendation; it recommends from a managed library.

## Backup Path

If Elasticsearch is not ready, continue the demo with SQL fallback. The result page should still work and explain the fallback path.

If external LLM configuration is unavailable, continue with the template-based gateway. The recommendation loop is still demonstrable because parsing, filtering, retrieval, plan creation, and feedback are local.

If Docker startup is slow, show the already-open browser session first, then explain the Docker Compose services after the demo loop.

## What To Avoid In The Demo

- Do not start with implementation details before showing the working loop.
- Do not promise medical diagnosis or rehabilitation treatment.
- Do not describe the project as a pure LLM chatbot.
- Do not use vague input such as "随便推荐一个" as the main demo case.
- Do not import new external data live unless the network and source are already verified.

## Short Explanation For Advisor Questions

### Why Java?

The project is built around `Spring Boot 3`, `Spring Security`, `MyBatis-Plus`, `PostgreSQL`, `Flyway`, `Thymeleaf`, and `LangChain4j`, which fits the graduation requirement and keeps the full stack maintainable in Java.

### Why not pure LLM?

Pure LLM recommendation is hard to control and hard to evaluate. FitMate uses rules and retrieval first, then uses the AI layer for parsing, explanation, and later expansion.

### What is the current innovation point?

The useful part is the closed recommendation loop: profile, natural-language request, safety filtering, hybrid retrieval, knowledge evidence, generated plan, completion feedback, and future personalization.

### What will be improved next?

Next steps are recommendation quality evaluation, better scoring visibility, richer admin tag governance, and thesis experiments comparing SQL-only, lexical retrieval, vector retrieval, and hybrid retrieval.
