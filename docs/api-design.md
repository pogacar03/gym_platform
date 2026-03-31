# API and Route Design

## Web Pages
- `GET /` home page
- `GET /login` login page
- `GET /dashboard` user dashboard
- `GET /profile` profile form
- `POST /profile` save profile
- `GET /videos` workout library
- `GET /recommendations` recommendation page
- `POST /recommendations` submit recommendation request
- `GET /admin/videos` admin video list
- `POST /admin/videos` save admin video

## Form Contracts

### Recommendation Form
- `requestText`: free-text workout request

### Profile Form
- `age`
- `gender`
- `heightCm`
- `weightKg`
- `fitnessGoal`
- `activityLevel`
- `availableEquipment`
- `injuryNotes`
- `kneeSensitive`
- `backSensitive`
- `exercisePreference`
- `weeklyFrequency`
- `preferredDurationMinutes`

### Admin Video Form
- `title`
- `description`
- `difficulty`
- `targetGoal`
- `targetBodyPart`
- `equipmentRequirement`
- `durationMinutes`
- `impactLevel`
- `safetyNotes`
- `videoUrl`
- `thumbnailUrl`

