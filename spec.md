# FitMate Graduation Project Spec

## 1. Project Vision
FitMate is an AI-driven fitness companion for university graduation project delivery. The system accepts natural-language exercise needs, understands user profile and safety constraints, searches a curated workout video library, and returns safe, personalized, video-based workout suggestions with explanation and follow-up tracking.

## 2. Target Users
- Student users who want a simple workout companion
- Demo users in graduation defense scenario
- Administrators who maintain workout content and review recommendation quality

## 3. Core Problem
Generic workout apps often recommend training content without fully considering time, equipment, injuries, and personal goals. This project focuses on combining structured safety rules with LLM-generated guidance so that users receive recommendations that are practical and explainable.

## 4. MVP Scope
### Included
- User registration and login
- User profile management
- Workout video library management
- Natural-language recommendation input
- Rule-based safety filtering
- SQL-based candidate retrieval
- LLM explanation generation with fallback
- Recommendation history persistence
- Workout completion logging

### Excluded In MVP
- Multi-tenant deployment
- Complex wearable device integration
- Real-time posture detection
- Streaming media server
- Advanced vector retrieval

## 5. Core User Flows
1. User signs in and completes health profile.
2. User describes workout intent in natural language.
3. System extracts structured constraints.
4. System filters unsafe workouts and retrieves matching videos.
5. System assembles a simple workout plan.
6. LLM generates recommendation explanation and caution notes.
7. User saves and completes the workout.

## 6. Functional Requirements
- The system must store user health constraints, available equipment, and workout goals.
- The system must prevent obviously unsafe recommendations for known risk conditions.
- The system must support admin CRUD for workout videos and metadata.
- The system must preserve recommendation history and workout logs.
- The system must degrade gracefully when LLM service is unavailable.

## 7. Non-Functional Requirements
- Clear web-based demo flow for defense presentation
- Maintainable Java codebase using Spring Boot and MyBatis-Plus
- Database schema managed through Flyway
- Swappable LLM provider abstraction

## 8. Acceptance Criteria
- A logged-in user can create or update a profile.
- A logged-in user can submit a workout request and receive recommendations.
- A knee-injury request does not return high-impact videos.
- The app still returns baseline recommendations when LLM is disabled.
- Admin can add a video and see it appear in the recommendation candidate set.

