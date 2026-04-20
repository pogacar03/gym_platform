# Next Iteration

This document defines the next practical optimization phase for FitMate. The goal is to improve both user experience and system robustness without losing the current stable MVP.

## Objective

Move the project from:

- a working thesis prototype

to:

- a smoother user-facing product
- a more efficient admin tool
- a more robust and defensible system for demo and thesis review

## P0: Core Experience And Stability

### 1. Upgrade the recommendation result page

Goal: make results feel like a personalized plan instead of a plain list.

Tasks:

- add a one-line summary at the top of the result
- show why each video matches the user
- highlight matched conditions such as `no equipment`, `chair`, `low impact`, `back-friendly`
- improve empty-result handling with fallback suggestions

Expected outcome:

- clearer recommendation reasoning
- better demo quality
- easier for users to trust the result

### 2. Add recommendation fallback handling

Goal: prevent recommendation failures from breaking the user flow.

Tasks:

- if LLM is unavailable, return rule-based recommendation
- if no fully matched video exists, return closest low-risk candidates
- if profile data is incomplete, show a guided prompt to complete required fields
- if import source fails, continue using approved existing content

Expected outcome:

- fewer dead ends
- more robust demo behavior
- safer user-facing experience

### 3. Strengthen data constraints and de-duplication

Goal: keep the video library clean and reliable over time.

Tasks:

- add a unique rule for `source_type + source_video_id`
- validate video URL and thumbnail URL format
- validate duration range
- make sure imported items cannot be approved twice
- ensure approved videos enter the main library only once

Expected outcome:

- no duplicate videos
- cleaner imports
- more stable recommendation results

## P1: Admin Workflow Improvements

### 4. Improve the pending review page

Goal: make admin review faster and more practical.

Tasks:

- show video thumbnail in the review list
- show suggested tags clearly
- show safety flags with stronger visual cues
- allow editing tags before approval
- highlight confidence score by color

Expected outcome:

- lower admin effort
- faster review process
- more usable import pipeline

### 5. Improve import source usability

Goal: reduce friction when configuring YouTube sources.

Tasks:

- allow pasting a full YouTube channel URL
- extract `channelId` automatically when possible
- add helper text for each source field
- show last import result summary
- show last run status as success or failed

Expected outcome:

- easier onboarding for admin users
- fewer misconfigured sources

### 6. Improve the video detail page

Goal: make each recommended video feel like a proper content item.

Tasks:

- show target muscle group
- show equipment requirement
- show posture and difficulty
- show safety note
- show intended audience

Expected outcome:

- richer browsing experience
- stronger recommendation transparency

## P1: Frontend UX Improvements

### 7. Improve recommendation input experience

Goal: reduce the burden of typing everything manually.

Tasks:

- add quick selectors for duration, posture, equipment, and intensity
- keep natural language as a flexible supplement
- sync selected chips with textarea content
- add 2 to 3 example requests

Expected outcome:

- easier first-time use
- faster request input

### 8. Refine the profile page

Goal: make user profiles more accurate and useful.

Tasks:

- split body areas into finer muscle regions
- add more precise training limitation options
- improve option labels to sound more natural
- continue simplifying structured selections

Expected outcome:

- better user profiling
- better matching quality

### 9. Unify visual style

Goal: improve product feel across the whole application.

Tasks:

- improve empty states
- improve error states
- unify button hierarchy
- review mobile layout
- unify card spacing and visual rhythm

Expected outcome:

- more polished UI
- stronger presentation quality

## P2: Robustness And Thesis Bonus Points

### 10. Complete access control checks

Tasks:

- prevent normal users from entering admin pages
- apply role checks on all admin endpoints
- redirect unauthenticated users to login
- protect sensitive actions more clearly

Expected outcome:

- safer access control
- better engineering quality

### 11. Expand automated tests

Tasks:

- add parsing tests
- add safety filtering tests
- add import de-duplication tests
- add admin permission tests
- add basic integration tests for recommendation flow

Expected outcome:

- stronger confidence before demo
- fewer regressions

### 12. Improve observability and troubleshooting

Tasks:

- add clearer logs for import failures
- add logs for recommendation fallback behavior
- add admin-visible summaries for recent failures
- improve user-facing error messages

Expected outcome:

- easier debugging
- easier maintenance

## Recommended Priority Order

If only a few items can be done next, use this order:

1. recommendation result page upgrade
2. data constraints and import de-duplication
3. pending review page enhancement
4. recommendation fallback handling
5. video detail page enhancement

## Suggested Supporting Documents

To support this phase, consider adding:

- `docs/error-handling.md`
- `docs/import-review-workflow.md`
- `docs/deploy-guide.md`

## Definition Of Success

This iteration is successful if:

- recommendation results are easier to understand
- admin review takes less effort
- duplicate and bad-quality import data are controlled
- the system can handle common failure scenarios gracefully
- the application feels more like a real product during demo
