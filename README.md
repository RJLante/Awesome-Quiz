

# Awesome-Quiz

**Awesome-Quiz** 是一个基于 Vue 3 + Spring Boot + Redis + RabbitMQ + ChatGLM (ZhiPu AI) + RxJava + SSE + JWT + ShardingSphere + MyBatis Plus + Caffeine/Redisson 的 **AI 答题应用平台**。支持 题目自动生成、AI 评分、实时流式推送 (SSE)、异步任务（消息队列）、角色权限、统计分析 等功能。

## 💻 安裝

### Git 安裝

在博客根目录里安装最新版【推荐】

```powershell
git clone -b main https://github.com/RJLante/Awesome-Quiz
```

## ⚙ 项目简介

**Awesome-Quiz** 致力于通过 AI 快速生成高质量题目与测评问卷，支持两类应用：

1. **得分类 (SCORE)**：每题仅一个正确答案，累积分数。
2. **测评类 (TEST)**：选项绑定性格 / 结果属性，最终计算匹配结果。

平台提供：

- 自助创建“应用” (App) → 设计题目 → 配置评分结果 → 发布给用户答题。
- 利用 **ChatGLM / 智谱 AI** 自动批量生成题目（同步 / 流式 / 异步三种模式）。
- 用户在线实时答题，获取分数 / 测评报告；管理员可审核、统计与运营。

![](https://awesomequiz-1345673117.cos.ap-shanghai.myqcloud.com/app_icon/1939353106849185794/eM8SvHKH.%E4%B8%BB%E9%A1%B5.png)

![](https://awesomequiz-1345673117.cos.ap-shanghai.myqcloud.com/app_icon/1939353106849185794/uhkY1Lqv.%E5%BA%94%E7%94%A8%E8%AF%A6%E6%83%85.png)

![](https://awesomequiz-1345673117.cos.ap-shanghai.myqcloud.com/app_icon/1939353106849185794/LYFZyrkZ.ai%E7%94%9F%E6%88%90%E9%A2%98%E7%9B%AE.png)



## 技术栈说明

| 层          | 选型                                          | 备注                                 |
| ----------- | --------------------------------------------- | ------------------------------------ |
| 前端        | Vue 3, TypeScript, Pinia, Vue Router, ECharts | 组件化、数据可视化、状态集中管理     |
| 构建        | Vue CLI 5 / npm                               | 支持代码分割、热更新                 |
| 后端框架    | Spring Boot 2.7.x                             | 快速开发、生态成熟                   |
| 安全        | Spring Security + JWT                         | 无状态认证、前后端分离               |
| ORM         | MyBatis Plus + MyBatis                        | CRUD 加速 + 自定义 SQL               |
| 数据分片    | Apache ShardingSphere                         | `user_answer_{0..1}` 按 `appId` 分表 |
| 消息队列    | RabbitMQ                                      | 解耦 & 异步生成长任务                |
| 缓存        | Redis + Caffeine                              | 二级缓存、热点数据提升命中率         |
| 限流 / 线程 | Redisson RateLimiter, 自定义 Scheduler        | 精细化限流 + VIP 独立线程池          |
| AI          | ChatGLM (ZhiPu) SDK `ClientV4`                | 流式 (RxJava Flowable) 和稳定模式    |
| 流式通信    | SSE (`SseEmitter`)                            | 题目生成过程实时输出                 |
| 任务        | Spring Scheduling + MQ                        | 定时清理 / 异步批处理                |
| 文档        | Swagger / Knife4j                             | 在线调试                             |
| 工具        | Hutool, EasyExcel, Caffeine, Lombok           | 提升开发效率                         |



## 业务流程

### 1. AI 题目生成三模式

| 模式         | 场景                 | 优点                     | 入口                                                         |
| ------------ | -------------------- | ------------------------ | ------------------------------------------------------------ |
| 同步一次性   | 少量题目 (≤10)       | 实现简单                 | `POST /question/ai_generate` (示例名称参考代码)              |
| 流式 SSE     | 中等题量，需实时体验 | 反馈及时，减少等待       | `GET /question/ai_generate/sse`                              |
| 异步 MQ 任务 | 大批量生成           | 不阻塞主线程，可失败重试 | `POST /question/ai_generate/async/mq` + `GET /task/{id}` 查询 |

### 2. 用户答题

选择应用 → 拉取题目 → 前端本地记录选择 → 提交 → 后端根据策略计算得分或属性命中 → 返回结果（分数或结果描述）。

### 3. 管理审核

管理员查看待审核应用 (`reviewStatus=0`) → 通过 / 拒绝 → 用户方展示上线应用。

### 4. 统计

定期或实时统计答题次数、正确率、结果分布，前端通过 ECharts 可视化。



### 环境要求

| 组件     | 推荐版本 | 说明                         |
| -------- | -------- | ---------------------------- |
| JDK      | 1.8+     | 已在 `pom.xml` 指定 1.8      |
| Node.js  | >= 16    | Vue CLI 5 兼容               |
| MySQL    | 8.x      | 初始化脚本位于 `backend/sql` |
| Redis    | 6.x      | 缓存 & 限流                  |
| RabbitMQ | 3.x      | 异步任务                     |
| Maven    | 3.6+     | 构建后端                     |

## 鸣谢 & License

- 基于社区优秀模板与组件（部分后端初始化模板来源 **程序员鱼皮** 的开源示例）。
- AI 能力由 **智谱 AI (ChatGLM)** 提供。
- 其它依赖详见 `pom.xml` / `package.json`。