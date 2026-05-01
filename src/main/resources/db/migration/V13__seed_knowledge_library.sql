INSERT INTO knowledge_document (title, source_type, source_name, language, topic, tags, status)
SELECT '低冲击椅子训练安全指南', 'GUIDELINE', 'FitMate Safety Library', 'zh_CN', '低冲击椅子训练', 'LOW_IMPACT,KNEE_SENSITIVE,CHAIR,SITTING', 'ACTIVE'
WHERE NOT EXISTS (SELECT 1 FROM knowledge_document WHERE title = '低冲击椅子训练安全指南' AND language = 'zh_CN');

INSERT INTO knowledge_document (title, source_type, source_name, language, topic, tags, status)
SELECT '膝盖敏感训练筛选原则', 'GUIDELINE', 'FitMate Safety Library', 'zh_CN', '膝盖保护', 'KNEE_SENSITIVE,LOW_IMPACT,NO_JUMPING', 'ACTIVE'
WHERE NOT EXISTS (SELECT 1 FROM knowledge_document WHERE title = '膝盖敏感训练筛选原则' AND language = 'zh_CN');

INSERT INTO knowledge_document (title, source_type, source_name, language, topic, tags, status)
SELECT '背部友好训练控制原则', 'GUIDELINE', 'FitMate Safety Library', 'zh_CN', '背部友好', 'BACK_SENSITIVE,MOBILITY,CONTROLLED_RANGE', 'ACTIVE'
WHERE NOT EXISTS (SELECT 1 FROM knowledge_document WHERE title = '背部友好训练控制原则' AND language = 'zh_CN');

INSERT INTO knowledge_document (title, source_type, source_name, language, topic, tags, status)
SELECT '无器械居家训练建议', 'GUIDELINE', 'FitMate Coaching Library', 'zh_CN', '无器械训练', 'EQUIPMENT_NONE,HOME_WORKOUT,SMALL_SPACE', 'ACTIVE'
WHERE NOT EXISTS (SELECT 1 FROM knowledge_document WHERE title = '无器械居家训练建议' AND language = 'zh_CN');

INSERT INTO knowledge_document (title, source_type, source_name, language, topic, tags, status)
SELECT '初学者训练强度控制', 'GUIDELINE', 'FitMate Coaching Library', 'zh_CN', '初学者训练', 'BEGINNER_FRIENDLY,LOW_IMPACT,RECOVERY', 'ACTIVE'
WHERE NOT EXISTS (SELECT 1 FROM knowledge_document WHERE title = '初学者训练强度控制' AND language = 'zh_CN');

INSERT INTO knowledge_document (title, source_type, source_name, language, topic, tags, status)
SELECT '热身与恢复建议', 'GUIDELINE', 'FitMate Coaching Library', 'zh_CN', '热身恢复', 'WARM_UP,RECOVERY,MOBILITY', 'ACTIVE'
WHERE NOT EXISTS (SELECT 1 FROM knowledge_document WHERE title = '热身与恢复建议' AND language = 'zh_CN');

INSERT INTO knowledge_document (title, source_type, source_name, language, topic, tags, status)
SELECT '训练反馈后的推荐调整', 'GUIDELINE', 'FitMate Recommendation Library', 'zh_CN', '反馈调节', 'FEEDBACK_ADJUSTMENT,TOO_HARD,TOO_EASY', 'ACTIVE'
WHERE NOT EXISTS (SELECT 1 FROM knowledge_document WHERE title = '训练反馈后的推荐调整' AND language = 'zh_CN');

INSERT INTO knowledge_document (title, source_type, source_name, language, topic, tags, status)
SELECT '长者友好训练注意事项', 'GUIDELINE', 'FitMate Safety Library', 'zh_CN', '长者训练', 'SENIOR_FRIENDLY,CHAIR,LOW_IMPACT,BALANCE', 'ACTIVE'
WHERE NOT EXISTS (SELECT 1 FROM knowledge_document WHERE title = '长者友好训练注意事项' AND language = 'zh_CN');

INSERT INTO knowledge_document (title, source_type, source_name, language, topic, tags, status)
SELECT 'Low-impact chair workout safety guide', 'GUIDELINE', 'FitMate Safety Library', 'en', 'low-impact chair workout', 'LOW_IMPACT,KNEE_SENSITIVE,CHAIR,SITTING', 'ACTIVE'
WHERE NOT EXISTS (SELECT 1 FROM knowledge_document WHERE title = 'Low-impact chair workout safety guide' AND language = 'en');

INSERT INTO knowledge_document (title, source_type, source_name, language, topic, tags, status)
SELECT 'Back-friendly workout principles', 'GUIDELINE', 'FitMate Safety Library', 'en', 'back-friendly training', 'BACK_SENSITIVE,MOBILITY,CONTROLLED_RANGE', 'ACTIVE'
WHERE NOT EXISTS (SELECT 1 FROM knowledge_document WHERE title = 'Back-friendly workout principles' AND language = 'en');

INSERT INTO knowledge_document (title, source_type, source_name, language, topic, tags, status)
SELECT 'Beginner intensity control', 'GUIDELINE', 'FitMate Coaching Library', 'en', 'beginner training', 'BEGINNER_FRIENDLY,LOW_IMPACT,RECOVERY', 'ACTIVE'
WHERE NOT EXISTS (SELECT 1 FROM knowledge_document WHERE title = 'Beginner intensity control' AND language = 'en');

INSERT INTO knowledge_document (title, source_type, source_name, language, topic, tags, status)
SELECT 'No-equipment home workout guidance', 'GUIDELINE', 'FitMate Coaching Library', 'en', 'no-equipment training', 'EQUIPMENT_NONE,HOME_WORKOUT,SMALL_SPACE', 'ACTIVE'
WHERE NOT EXISTS (SELECT 1 FROM knowledge_document WHERE title = 'No-equipment home workout guidance' AND language = 'en');

INSERT INTO knowledge_chunk (document_id, chunk_index, content, token_count, metadata_json, indexed)
SELECT id, 0, '膝盖敏感用户应优先选择低冲击训练，避免跳跃、快速深蹲和突然扭转。椅子训练可以降低平衡压力，但仍需要保持核心收紧、动作缓慢可控。如果训练中出现明显疼痛，应立即停止，并选择更温和的活动范围。推荐时应优先匹配坐姿、无器械或椅子辅助的视频。', 92, '{}', FALSE
FROM knowledge_document
WHERE title = '低冲击椅子训练安全指南' AND language = 'zh_CN'
  AND NOT EXISTS (
      SELECT 1 FROM knowledge_chunk WHERE document_id = knowledge_document.id AND chunk_index = 0
  );

INSERT INTO knowledge_chunk (document_id, chunk_index, content, token_count, metadata_json, indexed)
SELECT id, 0, '膝盖敏感并不代表完全不能练下肢，但推荐系统需要过滤跳跃、跑跳、有快速落地或频繁急停的高冲击内容。更合适的选择是低冲击步伐、坐姿训练、轻柔力量训练和可控制幅度的活动。如果用户提到膝盖痛、膝盖不好或膝盖敏感，应优先返回 LOW impact 视频。', 105, '{}', FALSE
FROM knowledge_document
WHERE title = '膝盖敏感训练筛选原则' AND language = 'zh_CN'
  AND NOT EXISTS (
      SELECT 1 FROM knowledge_chunk WHERE document_id = knowledge_document.id AND chunk_index = 0
  );

INSERT INTO knowledge_chunk (document_id, chunk_index, content, token_count, metadata_json, indexed)
SELECT id, 0, '背部敏感用户更适合选择节奏稳定、躯干支撑明确、动作幅度可控的训练。推荐时应降低高强度卷腹、快速扭转、长时间平板支撑等高核心负荷内容的优先级。更好的解释方式是提醒用户保持脊柱中立、不要憋气、以支撑感和延展感为主。', 96, '{}', FALSE
FROM knowledge_document
WHERE title = '背部友好训练控制原则' AND language = 'zh_CN'
  AND NOT EXISTS (
      SELECT 1 FROM knowledge_chunk WHERE document_id = knowledge_document.id AND chunk_index = 0
  );

INSERT INTO knowledge_chunk (document_id, chunk_index, content, token_count, metadata_json, indexed)
SELECT id, 0, '无器械居家训练应优先考虑空间、噪音和安全性。适合小空间的内容包括站姿低冲击有氧、坐姿训练、徒手上肢、核心稳定和灵活性训练。如果用户明确没有器械，推荐系统不应返回依赖哑铃、弹力带或固定器械的视频，除非该视频可以无器械替代。', 92, '{}', FALSE
FROM knowledge_document
WHERE title = '无器械居家训练建议' AND language = 'zh_CN'
  AND NOT EXISTS (
      SELECT 1 FROM knowledge_chunk WHERE document_id = knowledge_document.id AND chunk_index = 0
  );

INSERT INTO knowledge_chunk (document_id, chunk_index, content, token_count, metadata_json, indexed)
SELECT id, 0, '初学者推荐应优先保证可完成性，而不是追求训练强度。系统可以选择较短时长、低冲击、动作解释清楚、转换节奏慢的视频。解释中应告诉用户可以降低幅度、减少组数或暂停休息。如果用户上次反馈太难，下次推荐应降低冲击、缩短时长或转向恢复类内容。', 98, '{}', FALSE
FROM knowledge_document
WHERE title = '初学者训练强度控制' AND language = 'zh_CN'
  AND NOT EXISTS (
      SELECT 1 FROM knowledge_chunk WHERE document_id = knowledge_document.id AND chunk_index = 0
  );

INSERT INTO knowledge_chunk (document_id, chunk_index, content, token_count, metadata_json, indexed)
SELECT id, 0, '每次训练前建议安排 2 到 5 分钟热身，优先活动肩、髋、膝、踝等关节。训练后可以选择轻柔拉伸、呼吸恢复和低强度活动，帮助心率逐步下降。恢复训练不应被解释为没有效果，它适合疲劳、久坐、初学者和需要降低负担的用户。', 91, '{}', FALSE
FROM knowledge_document
WHERE title = '热身与恢复建议' AND language = 'zh_CN'
  AND NOT EXISTS (
      SELECT 1 FROM knowledge_chunk WHERE document_id = knowledge_document.id AND chunk_index = 0
  );

INSERT INTO knowledge_chunk (document_id, chunk_index, content, token_count, metadata_json, indexed)
SELECT id, 0, '用户反馈是个性化推荐的重要信号。如果用户反馈太难，应优先降低冲击等级、缩短时长、选择初级或恢复内容。如果用户反馈太轻松，可以适度增加时长、选择中等强度或更有力量训练成分的视频。如果用户反馈刚好，应保持当前训练方向，避免突然大幅改变。', 99, '{}', FALSE
FROM knowledge_document
WHERE title = '训练反馈后的推荐调整' AND language = 'zh_CN'
  AND NOT EXISTS (
      SELECT 1 FROM knowledge_chunk WHERE document_id = knowledge_document.id AND chunk_index = 0
  );

INSERT INTO knowledge_chunk (document_id, chunk_index, content, token_count, metadata_json, indexed)
SELECT id, 0, '长者友好训练应优先考虑平衡、安全和动作可理解性。椅子辅助、坐姿训练、低冲击步伐和缓慢灵活性训练通常更合适。推荐说明应提醒用户保持旁边有支撑物，避免快速转身和突然改变方向。如果用户有明显疼痛、头晕或胸闷，应立即停止训练。', 101, '{}', FALSE
FROM knowledge_document
WHERE title = '长者友好训练注意事项' AND language = 'zh_CN'
  AND NOT EXISTS (
      SELECT 1 FROM knowledge_chunk WHERE document_id = knowledge_document.id AND chunk_index = 0
  );

INSERT INTO knowledge_chunk (document_id, chunk_index, content, token_count, metadata_json, indexed)
SELECT id, 0, 'For knee-sensitive users, prioritize low-impact videos and avoid jumping, fast deep squats, abrupt stops, and rapid twisting. Chair workouts can reduce balance demand, but users should still keep movement slow, controlled, and comfortable. If pain increases, the recommendation should advise stopping and switching to a smaller range of motion.', 66, '{}', FALSE
FROM knowledge_document
WHERE title = 'Low-impact chair workout safety guide' AND language = 'en'
  AND NOT EXISTS (
      SELECT 1 FROM knowledge_chunk WHERE document_id = knowledge_document.id AND chunk_index = 0
  );

INSERT INTO knowledge_chunk (document_id, chunk_index, content, token_count, metadata_json, indexed)
SELECT id, 0, 'Back-friendly workouts should favor stable pacing, supported positions, neutral spine cues, and controlled range of motion. High-load core moves, fast twisting, and long static holds should be de-prioritized when the user reports back sensitivity. The explanation should emphasize support, breathing, and stopping if pain increases.', 61, '{}', FALSE
FROM knowledge_document
WHERE title = 'Back-friendly workout principles' AND language = 'en'
  AND NOT EXISTS (
      SELECT 1 FROM knowledge_chunk WHERE document_id = knowledge_document.id AND chunk_index = 0
  );

INSERT INTO knowledge_chunk (document_id, chunk_index, content, token_count, metadata_json, indexed)
SELECT id, 0, 'Beginner recommendations should optimize for completion and confidence before intensity. Shorter sessions, lower impact, clear follow-along pacing, and slower transitions are usually safer. If recent feedback says the last plan was too hard, the next recommendation should reduce impact, shorten duration, or use recovery-focused content.', 57, '{}', FALSE
FROM knowledge_document
WHERE title = 'Beginner intensity control' AND language = 'en'
  AND NOT EXISTS (
      SELECT 1 FROM knowledge_chunk WHERE document_id = knowledge_document.id AND chunk_index = 0
  );

INSERT INTO knowledge_chunk (document_id, chunk_index, content, token_count, metadata_json, indexed)
SELECT id, 0, 'No-equipment home workouts should respect space, noise, and safety constraints. Good candidates include low-impact standing cardio, chair workouts, bodyweight upper-body sessions, core stability, and mobility routines. If the user says no equipment, videos requiring dumbbells, bands, or machines should not be selected unless a no-equipment alternative is clearly available.', 67, '{}', FALSE
FROM knowledge_document
WHERE title = 'No-equipment home workout guidance' AND language = 'en'
  AND NOT EXISTS (
      SELECT 1 FROM knowledge_chunk WHERE document_id = knowledge_document.id AND chunk_index = 0
  );
