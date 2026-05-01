# FitMate Next Phase Plan

## Goal
把当前版本从“功能完整的毕设原型”继续推进成“稳定、可解释、可持续优化的智能健身推荐系统”。

---

## P0 Must Do

### 1. 收口当前版本
- 整理并提交当前 `Elasticsearch + hybrid retrieval + knowledge enhancement` 改动
- 保持业务代码和辅助工具文件分离
- 补一份阶段说明文档，记录：
  - 当前推荐链路
  - 搜索链路
  - 导入与标签治理链路
  - 已知限制

### 2. 推荐排序可解释化
- 为每个候选视频保留并记录：
  - lexical score
  - vector score
  - business rule bonus
  - safety penalty
  - final rank score
- 推荐结果支持展示“为什么排在前面”
- 后台可查看推荐得分明细

### 3. 用户反馈闭环
- 增加训练计划反馈：
  - too easy
  - just right
  - too hard
- 增加视频反馈：
  - liked
  - not suitable
  - not interested
- 将反馈用于后续推荐排序调整

### 4. 搜索与推荐监控
- 记录推荐请求是否：
  - 使用 hybrid retrieval
  - 走 SQL fallback
  - 放宽了 goal
- 统计：
  - fallback ratio
  - vector hit ratio
  - zero-result request patterns

---

## P1 High Value

### 5. 标签质量治理
- 后台增加标签质量面板
- 支持按问题类型筛选：
  - missing duration
  - missing impact
  - missing difficulty
  - missing extra tags
- 增加批量修复能力
- 展示标签覆盖率趋势

### 6. 导入源质量治理
- source 健康状态面板
- source 失败原因记录
- source 审核通过率
- source 推荐命中率
- source 导入量与有效量对比

### 7. 推荐结果产品化
- 每个视频卡片增加“首选理由”
- 推荐结果增加建议顺序
- 展示系统本次采用的画像条件
- 区分：
  - system explanation
  - video-level reason
  - coach notes

### 8. Dashboard 深化
- 下一次建议训练
- 最近反馈摘要
- 本周目标完成状态
- 最近一次推荐主题与命中方式

---

## P2 Resume / Thesis Boost

### 9. 更强的知识增强
- 将当前静态 knowledge snippets 升级成结构化知识库
- 支持知识类别：
  - recovery
  - knee safety
  - back safety
  - beginner guidance
  - low-impact guidance
- 结果页展示知识来源分类

### 10. 外部 embedding 能力替换
- 将当前本地 demo embedding 抽象成 provider
- 后续接真实 embedding model
- 比较：
  - lexical only
  - vector only
  - hybrid retrieval

### 11. 推荐效果分析
- 记录最常见请求类型
- 分析高点击 / 高满意度视频
- 分析低命中标签组合
- 形成可写进论文和简历的结果对比

---

## Recommended Order

1. 收口当前版本
2. 推荐排序可解释化
3. 用户反馈闭环
4. 搜索与推荐监控
5. 标签质量治理
6. 导入源质量治理
7. 推荐结果产品化
8. Dashboard 深化
9. 更强的知识增强
10. 外部 embedding 替换
11. 推荐效果分析

---

## Notes

- 当前项目已经具备：
  - PostgreSQL 主业务库
  - Elasticsearch 混合检索
  - 导入审核与标签治理
  - 用户画像与推荐闭环
  - 中英双语
- 下一阶段重点不是继续堆功能，而是：
  - 收口
  - 做深
  - 做成闭环
