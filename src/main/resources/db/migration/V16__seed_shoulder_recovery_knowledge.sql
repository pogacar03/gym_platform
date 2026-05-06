INSERT INTO knowledge_document (title, source_type, source_name, language, topic, tags, status)
SELECT '肩部不适训练筛选原则', 'GUIDELINE', 'FitMate Safety Library', 'zh_CN', '肩部康复训练', 'SHOULDER_SENSITIVE,RECOVERY,MOBILITY,LOW_IMPACT', 'ACTIVE'
WHERE NOT EXISTS (SELECT 1 FROM knowledge_document WHERE title = '肩部不适训练筛选原则' AND language = 'zh_CN');

INSERT INTO knowledge_chunk (document_id, chunk_index, content, token_count, metadata_json, indexed)
SELECT id, 0, '用户提到肩周炎、肩痛、肩部僵硬或肩膀不舒服时，推荐系统应优先选择上肢活动度、轻柔拉伸、肩胛稳定和恢复类内容。应避免高强度推举、爆发甩臂、大重量肩部训练和会明显诱发疼痛的动作。推荐解释中需要提醒用户保持无痛范围、动作缓慢可控，疼痛加重时立即停止。', 116, '{}', FALSE
FROM knowledge_document
WHERE title = '肩部不适训练筛选原则' AND language = 'zh_CN'
  AND NOT EXISTS (
      SELECT 1 FROM knowledge_chunk WHERE document_id = knowledge_document.id AND chunk_index = 0
  );

INSERT INTO knowledge_document (title, source_type, source_name, language, topic, tags, status)
SELECT 'Shoulder discomfort workout screening', 'GUIDELINE', 'FitMate Safety Library', 'en', 'shoulder recovery training', 'SHOULDER_SENSITIVE,RECOVERY,MOBILITY,LOW_IMPACT', 'ACTIVE'
WHERE NOT EXISTS (SELECT 1 FROM knowledge_document WHERE title = 'Shoulder discomfort workout screening' AND language = 'en');

INSERT INTO knowledge_chunk (document_id, chunk_index, content, token_count, metadata_json, indexed)
SELECT id, 0, 'When a user mentions frozen shoulder, shoulder pain, shoulder stiffness, or rotator cuff discomfort, recommendations should favor upper-body mobility, gentle stretching, scapular control, and recovery-focused videos. Avoid heavy pressing, explosive arm swings, high-load shoulder work, or movements that increase pain. Guidance should remind the user to stay in a pain-free range and stop if symptoms worsen.', 92, '{}', FALSE
FROM knowledge_document
WHERE title = 'Shoulder discomfort workout screening' AND language = 'en'
  AND NOT EXISTS (
      SELECT 1 FROM knowledge_chunk WHERE document_id = knowledge_document.id AND chunk_index = 0
  );
