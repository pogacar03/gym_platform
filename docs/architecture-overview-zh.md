# FitMate 系统架构说明

FitMate 是一个基于 Java 的个性化健身推荐平台。用户可以维护训练画像，用自然语言描述训练需求，系统从经过整理的视频内容库中推荐更安全、更合适的训练视频，并记录训练完成情况和主观反馈。管理员负责维护训练内容库、导入 YouTube 频道内容、审核系统建议标签，并持续提升推荐内容质量。

## 1. 技术栈

| 层级 | 技术 | 职责 |
| --- | --- | --- |
| 页面渲染 | Thymeleaf、Bootstrap、原生 JavaScript | 服务端页面渲染、表单交互、语言切换、轻量前端交互 |
| Web 框架 | Spring Boot 3 | MVC 路由、依赖注入、配置管理、应用生命周期 |
| 安全认证 | Spring Security | 登录、记住登录、基于角色的访问控制 |
| 数据访问 | MyBatis-Plus、MyBatis XML | 常规 CRUD 和复杂查询 |
| 数据库 | PostgreSQL 16 | 用户、画像、视频、计划、日志、导入记录、推荐历史 |
| 数据库迁移 | Flyway | 表结构版本管理和初始化数据 |
| 检索服务 | Elasticsearch 8 | 视频关键词检索和语义候选召回 |
| 大模型接入边界 | LangChain4j 依赖 + `LlmGateway` 抽象 | 推荐解释生成和可替换的大模型提供方边界 |
| 本地 AI 支撑 | `TemplateLlmGateway`、`EmbeddingService`、知识库 JSON | 离线解释降级、本地向量生成、训练建议知识片段 |
| 部署 | Docker Compose + Maven | 本地 PostgreSQL、Elasticsearch 和 Spring Boot 应用运行 |

## 2. 系统总体架构图

```mermaid
flowchart LR
    User["用户浏览器"] --> Thymeleaf["Thymeleaf 页面"]
    Admin["管理员浏览器"] --> Thymeleaf

    Thymeleaf --> Controllers["Spring MVC Controller"]
    Controllers --> Services["业务 Service"]
    Services --> Mappers["MyBatis-Plus Mapper"]
    Mappers --> PostgreSQL[("PostgreSQL 主数据库")]

    Services --> SearchService["WorkoutVideoSearchService"]
    SearchService --> Elasticsearch[("Elasticsearch 视频索引")]

    Services --> LlmGateway["LlmGateway"]
    LlmGateway --> TemplateGateway["TemplateLlmGateway / LangChain4j 边界"]

    Services --> Knowledge["训练知识 JSON"]
    Services --> ImportPipeline["视频导入流水线"]
    ImportPipeline --> YouTube["YouTube RSS Feed"]
    ImportPipeline --> PostgreSQL
    ImportPipeline --> Elasticsearch
```

系统采用分层单体架构。这样既方便毕设阶段开发、部署和讲解，也保留了清晰的模块边界：Controller 不直接访问数据库，推荐逻辑集中在 Service 层，搜索和大模型能力通过接口隔离，后续可以替换为更复杂的实现。

## 3. 代码包结构图

```mermaid
flowchart TB
    Root["com.graduation.fitmate"]
    Root --> Config["config<br/>安全、LLM、搜索配置"]
    Root --> Controller["controller<br/>MVC 请求处理"]
    Root --> Service["service<br/>核心业务逻辑"]
    Root --> Mapper["mapper<br/>MyBatis-Plus 数据访问"]
    Root --> Entity["entity<br/>数据库实体"]
    Root --> DTO["dto<br/>表单对象和视图模型"]
    Root --> LLM["llm<br/>大模型网关抽象"]
    Root --> Search["search<br/>Elasticsearch 文档模型"]
    Root --> Util["util<br/>页面显示辅助工具"]
```

## 4. MVC 请求处理流程

```mermaid
sequenceDiagram
    participant Browser as 浏览器
    participant Controller as Controller
    participant Service as Service
    participant Mapper as Mapper
    participant DB as PostgreSQL
    participant View as Thymeleaf

    Browser->>Controller: 发起 HTTP 请求
    Controller->>Service: 校验输入并调用业务方法
    Service->>Mapper: 查询或保存数据
    Mapper->>DB: 执行 SQL
    DB-->>Mapper: 返回数据行
    Mapper-->>Service: 返回实体对象
    Service-->>Controller: 返回 DTO / ViewModel
    Controller-->>View: 注入页面模型
    View-->>Browser: 渲染 HTML 页面
```

典型页面流程：

- `HomeController`：渲染仪表盘，记录训练完成和反馈。
- `ProfileController`：维护用户训练画像。
- `RecommendationController`：接收快捷筛选和自然语言训练需求。
- `WorkoutVideoController`：展示视频列表和视频详情。
- `AdminVideoController`：管理训练视频和批量补标签。
- `AdminImportController`：管理频道导入、待发布视频审核。

## 5. 登录认证与权限控制

```mermaid
flowchart TD
    Start["用户访问受保护页面"] --> AuthCheck{"是否已登录？"}
    AuthCheck -- 否 --> Login["跳转到 /login"]
    Login --> Credential["提交用户名和密码"]
    Credential --> Provider["DaoAuthenticationProvider"]
    Provider --> UserDetails["CustomUserDetailsService"]
    UserDetails --> Account[("user_account")]
    Provider --> Success{"凭据是否正确？"}
    Success -- 否 --> LoginError["显示登录错误"]
    Success -- 是 --> Session["创建 Session / Remember-me Token"]
    Session --> RoleCheck{"是否访问 /admin/**？"}
    AuthCheck -- 是 --> RoleCheck
    RoleCheck -- 否 --> UserPage["用户端页面"]
    RoleCheck -- 是 --> AdminRole{"是否具备 ROLE_ADMIN？"}
    AdminRole -- 是 --> AdminPage["后台管理页面"]
    AdminRole -- 否 --> Denied["403 Forbidden"]
```

权限规则：

- 公开访问：`/`、`/login`、`/register`、`/css/**`
- 普通登录用户：仪表盘、个人资料、视频库、训练推荐
- 管理员：`/admin/**`
- 记住登录：使用 `persistent_logins` 表保存 token
- 页面导航：普通用户不显示后台入口，管理员才显示“管理”和“导入”

## 6. 推荐核心流程

```mermaid
flowchart TD
    A["用户提交训练请求<br/>快捷筛选 + 自然语言"] --> B["RecommendationController"]
    B --> C["RequestParsingService<br/>提取时长、姿势、器械、目标、部位、安全限制"]
    C --> D["读取用户训练画像"]
    D --> E["结合最近反馈进行调整<br/>太轻松 / 刚好 / 太难"]
    E --> F["安全规则过滤<br/>排除不匹配器械、高冲击、敏感部位风险"]
    F --> G["WorkoutVideoSearchService"]
    G --> H["Elasticsearch 关键词检索"]
    G --> I["Elasticsearch 语义候选召回"]
    H --> J["合并并归一化候选分数"]
    I --> J
    J --> K["从 PostgreSQL 读取候选视频"]
    K --> L["业务排序<br/>目标、时长、姿势、器械、冲击等级"]
    L --> M["无合适结果时启用降级策略"]
    M --> N["生成 WorkoutPlan 和 WorkoutPlanItem"]
    L --> N
    N --> O["RecommendationKnowledgeService<br/>匹配训练建议"]
    O --> P["LlmGateway<br/>生成推荐说明"]
    P --> Q["RecommendationResult"]
    Q --> R["Thymeleaf 推荐结果页"]
```

这个系统没有让大模型直接决定视频结果。推荐链路先通过结构化解析、安全规则、数据库和 Elasticsearch 检索、确定性排序生成候选结果，最后再生成解释文案。这样推荐结果更可控，也更容易在答辩中解释安全性和可降级能力。

## 7. 检索与排序设计

```mermaid
flowchart LR
    Request["ParsedRecommendationRequest"] --> QueryBuilder["检索条件构建"]
    QueryBuilder --> Lexical["关键词字段<br/>标题、简介、标签、频道"]
    QueryBuilder --> Vector["Embedding 向量<br/>本地 token 向量"]
    Lexical --> ES[("Elasticsearch")]
    Vector --> ES
    ES --> Scores["SearchCandidateScore<br/>关键词分数 + 语义分数"]
    Scores --> Merge["加权合并<br/>lexicalWeight + vectorWeight"]
    Merge --> CandidateIds["候选视频 ID"]
    CandidateIds --> DB[("PostgreSQL workout_video")]
    DB --> BusinessScore["业务匹配评分"]
    BusinessScore --> FinalList["最终推荐视频列表"]
```

当前检索特点：

- 关键词检索适合匹配明确需求，例如“椅子训练”“低冲击”“背部”。
- 语义候选召回可以提升相近表达的召回能力。
- 候选分数会展示在推荐结果中，用于解释“为什么排在这里”。
- Elasticsearch 不可用或没有安全候选时，会回退到数据库检索。

## 8. 内容导入流程

```mermaid
sequenceDiagram
    participant Admin as 管理员
    participant ImportController as AdminImportController
    participant ImportService as VideoImportService
    participant YouTube as YouTube RSS
    participant Tagger as ImportedVideoTaggingService
    participant DB as PostgreSQL
    participant Index as Elasticsearch Index

    Admin->>ImportController: 新增来源 / 立即导入
    ImportController->>ImportService: importFromSource(sourceId)
    ImportService->>YouTube: 拉取频道 feed
    YouTube-->>ImportService: 返回视频元数据
    ImportService->>Tagger: 推断目标、部位、器械、姿势、冲击等级、扩展标签
    Tagger-->>ImportService: 返回建议标签和置信度
    ImportService->>DB: 写入或更新 imported_video
    Admin->>ImportController: 审核并发布视频
    ImportController->>ImportService: approveImportedVideo()
    ImportService->>DB: 写入或更新 workout_video
    ImportService->>Index: 同步搜索索引
```

这条流水线减少了手动录入成本。系统自动获取标题、简介、缩略图、来源链接，并推断标签；管理员保留最终审核权，审核通过后才发布到正式训练内容库。

## 9. 用户反馈闭环

```mermaid
flowchart TD
    A["用户完成最新计划"] --> B["写入 workout_log"]
    B --> C["用户反馈训练感受<br/>太轻松 / 刚好 / 太难"]
    C --> D["DashboardService 保存反馈"]
    D --> E["RecommendationService 读取最近反馈"]
    E --> F{"反馈类型"}
    F -- "太难" --> G["缩短时长，优先低冲击 / 初级 / 恢复训练"]
    F -- "太轻松" --> H["适当增加时长，允许中等强度内容"]
    F -- "刚好" --> I["保持相近训练方向"]
    G --> J["影响下一次推荐"]
    H --> J
    I --> J
```

这个反馈闭环让仪表盘不只是展示页面。用户的训练完成情况和主观反馈会影响下一次推荐，使系统从“一次性推荐”变成“持续训练助手”。

## 10. 数据库 ER 图

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

## 11. 部署图

```mermaid
flowchart TB
    subgraph LocalMachine["本地开发 / 单机部署环境"]
        Browser["浏览器"]
        App["Spring Boot 应用<br/>localhost:8080"]
        Postgres["PostgreSQL 16<br/>Docker: fitmate-postgres<br/>localhost:5432"]
        ES["Elasticsearch 8<br/>Docker: fitmate-elasticsearch<br/>localhost:9200"]
        Maven["Maven 运行环境"]
    end

    Browser --> App
    Maven --> App
    App --> Postgres
    App --> ES
    App --> YouTube["YouTube RSS Feed"]
    App -. 可选 .-> LLM["外部大模型服务<br/>通过 LangChain4j 边界接入"]
```

开发启动流程：

1. `docker compose up -d`
2. `mvn spring-boot:run`
3. 打开 `http://localhost:8080`

## 12. 核心组件职责

| 模块 | 主要类 | 职责 |
| --- | --- | --- |
| 登录认证 | `SecurityConfig`、`CustomUserDetailsService`、`RegistrationService` | 登录、角色权限、记住登录、注册 |
| 仪表盘 | `HomeController`、`DashboardService`、`DashboardView` | 今日进度、本周节奏、近期记录、训练反馈 |
| 用户画像 | `ProfileController`、`UserProfileService`、`UserProfile` | 训练目标、器械、目标部位、安全备注 |
| 视频库 | `WorkoutVideoController`、`WorkoutVideoService`、`WorkoutVideo` | 用户端视频浏览和详情展示 |
| 推荐 | `RecommendationController`、`RecommendationService`、`RequestParsingService` | 请求解析、安全过滤、检索排序、保存计划 |
| 搜索 | `WorkoutVideoSearchService`、`ElasticsearchWorkoutVideoSearchGateway`、`EmbeddingService` | 混合召回、候选评分、排序解释 |
| 训练知识 | `RecommendationKnowledgeService`、`RecommendationKnowledgeNote` | 根据请求和画像匹配训练建议 |
| 大模型边界 | `LlmGateway`、`TemplateLlmGateway` | 推荐解释生成和降级输出 |
| 内容导入 | `AdminImportController`、`VideoImportService`、`ImportedVideoTaggingService` | YouTube feed 导入、自动标签建议、审核发布 |
| 后台视频管理 | `AdminVideoController`、`WorkoutVideoService` | 手动维护视频、批量补标签 |

## 13. 架构优势

FitMate 的架构重点是可靠性、可解释性和可维护性，而不是让大模型完全自动决策。健身推荐涉及身体安全，因此系统先用规则和结构化数据控制候选范围，再通过检索和排序选择视频，最后才生成用户可读的推荐说明。

主要优势：

- 用户端和管理端职责清晰，普通用户不会看到后台入口。
- Spring Security 提供基于角色的访问控制。
- PostgreSQL 作为主数据源，保证业务数据可靠落地。
- Elasticsearch 提升内容召回能力，同时保留数据库降级路径。
- 推荐历史、训练计划和训练日志形成用户闭环。
- 内容导入流水线降低视频库维护成本。
- Flyway 保证数据库结构可以在不同机器上稳定复现。

