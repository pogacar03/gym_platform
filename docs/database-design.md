# Database Design

## Tables

### `user_account`
- `id` bigint primary key
- `username` varchar unique
- `password_hash` varchar
- `role` varchar
- `display_name` varchar
- `enabled` boolean
- `created_at` timestamp
- `updated_at` timestamp

### `user_profile`
- `id` bigint primary key
- `user_id` bigint unique
- `age` integer
- `gender` varchar
- `height_cm` integer
- `weight_kg` decimal
- `fitness_goal` varchar
- `activity_level` varchar
- `available_equipment` varchar
- `injury_notes` varchar
- `knee_sensitive` boolean
- `back_sensitive` boolean
- `exercise_preference` varchar
- `weekly_frequency` integer
- `preferred_duration_minutes` integer
- `created_at` timestamp
- `updated_at` timestamp

### `workout_video`
- `id` bigint primary key
- `title` varchar
- `description` text
- `difficulty` varchar
- `target_goal` varchar
- `target_body_part` varchar
- `equipment_requirement` varchar
- `duration_minutes` integer
- `impact_level` varchar
- `safety_notes` varchar
- `video_url` varchar
- `thumbnail_url` varchar
- `active` boolean
- `created_at` timestamp
- `updated_at` timestamp

### `workout_tag`
- `id` bigint primary key
- `name` varchar unique

### `workout_video_tag`
- `video_id` bigint
- `tag_id` bigint

### `recommendation_history`
- `id` bigint primary key
- `user_id` bigint
- `request_text` text
- `parsed_goal` varchar
- `parsed_duration_minutes` integer
- `parsed_equipment` varchar
- `safety_flags` varchar
- `explanation` text
- `created_at` timestamp

### `workout_plan`
- `id` bigint primary key
- `user_id` bigint
- `recommendation_id` bigint
- `title` varchar
- `summary` text
- `created_at` timestamp

### `workout_plan_item`
- `id` bigint primary key
- `plan_id` bigint
- `video_id` bigint
- `sort_order` integer
- `sets_count` integer
- `reps_or_duration` varchar

### `workout_log`
- `id` bigint primary key
- `user_id` bigint
- `plan_id` bigint
- `status` varchar
- `fatigue_level` integer
- `feedback_note` varchar
- `completed_at` timestamp

## Initial Indexes
- `idx_workout_video_goal`
- `idx_workout_video_equipment`
- `idx_recommendation_history_user_id`
- `idx_workout_log_user_id`

