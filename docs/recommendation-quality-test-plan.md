# Recommendation Quality Test Plan

This checklist is used to test FitMate as a real user, not only as an API. A recommendation is considered good only when the request is meaningful, the parsed intent is reasonable, Elasticsearch participates when matching content exists, and the returned videos are safe, relevant, and follow-along friendly.

## What To Check

- Input validation: meaningless text such as only numbers should not generate a workout plan.
- Intent parsing: the system should identify goal, body area, safety limitation, equipment, posture, duration, and impact level when they are present.
- Retrieval path: normal requests should prefer hybrid Elasticsearch retrieval. SQL fallback should be rare and clearly explained.
- Safety filtering: sensitive knee, back, shoulder, senior, or pain-related requests should avoid high-risk videos.
- Video relevance: returned videos should match the requested body area and goal, and should be real follow-along workout content.
- Knowledge grounding: safety or rehab requests should include relevant knowledge references.
- Result quality: the titles, explanations, tags, thumbnails, and score breakdown should make sense to a user.

## Core Test Cases

| Case | User Input | Expected Behavior |
| --- | --- | --- |
| Invalid numeric | `123` | Reject with a clear message. No videos should be returned. |
| Vague request | `随便给我推荐一个` | Ask user to provide goal, body area, limitation, duration, or equipment. |
| Shoulder recovery | `我有一点肩周炎，想缓解一下，怎么训练？` | Parse as recovery, shoulder-sensitive, low-impact upper-body/mobility. Return shoulder/upper-body follow-along videos and shoulder safety knowledge. |
| Knee low impact | `我膝盖不好，想做20分钟无器械低冲击减脂训练` | Exclude high-impact videos. Prefer no-equipment low-impact walking/cardio or chair-safe options. |
| Seated chair workout | `我想坐在椅子上做15分钟训练，动作要温和` | Return seated or chair-supported videos. Avoid standing-only results when enough seated content exists. |
| Back pain stretch | `我腰背不太舒服，想做背部放松和拉伸，不要太累` | Prefer back/hip mobility and stretching. Avoid core-heavy or unrelated news/explainer content. |
| No-equipment weight loss | `我没有器械，想在家做20分钟减脂训练` | Return multiple no-equipment home workout options. Avoid dumbbell/band-only videos. |
| Dumbbell upper body | `我有哑铃，想练上肢和肩臂，20分钟左右，稍微有挑战` | Return upper-body dumbbell or bodyweight alternatives. Avoid unrelated low-intensity rehab unless safety flags exist. |
| Senior gentle | `给老人推荐一个10分钟温和的坐姿训练` | Prefer senior-friendly, low-impact, seated videos around 10 minutes. |
| Conflicting unsafe request | `我膝盖疼但想做高强度跳跃HIIT，30分钟` | Safety should win. Explain that jumping/high-impact HIIT is filtered and return safer alternatives. |

## Current Findings From Local Run

- Passed: numeric-only input is rejected.
- Passed: vague input is rejected, but the error message still mentions numbers too strongly and should be generalized.
- Passed: shoulder recovery now uses hybrid retrieval instead of SQL fallback, with keyword and semantic hits.
- Passed: shoulder recovery includes `肩部不适训练筛选原则`.
- Needs improvement: back-pain and unsafe-HIIT cases can still return news/explainer content, which should be excluded from user recommendations.
- Needs improvement: no-equipment weight-loss returned only one final video in the tested run; the system should relax non-safety constraints to keep three usable options.
- Needs improvement: seated senior requests may return videos that are only loosely senior/seated related. Ranking should prefer explicit seated/chair/senior metadata.

## Suggested Quality Gates

- For normal recommendation requests, at least two returned videos should come from hybrid ES retrieval before SQL fallback is acceptable.
- Returned videos must have `duration_minutes` and a playable `video_url`.
- Returned videos should contain follow-along signals such as workout, exercise, routine, follow along, stretch, mobility, minute, or equivalent metadata.
- Pain or rehab requests must include a relevant safety knowledge reference.
- Safety flags must override user requests for high-impact or risky training.
- Each supported demo scenario should return at least two relevant videos.
