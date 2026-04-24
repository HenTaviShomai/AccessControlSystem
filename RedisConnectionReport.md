针对这次 Redis 连接故障，如果从 Spring Boot 4.0.5 的技术特性进行深度复盘，这本质上是一场由**版本演进导致的“配置命名空间截断”事故**。

以下是详细的错误分析报告：

---

## 🚩 Redis 连接异常复盘报告 (Technical Incident Report)

### 1. 故障现象描述 (Symptom)
在 Docker 容器化部署环境中，尽管 `application-docker.yml` 明确配置了 `spring.redis.host: redis`，且通过 `docker-compose` 注入了 `SPRING_REDIS_HOST=redis`，但应用在启动及运行过程中，依然顽固地尝试连接 **`localhost:6379`**。

由于容器内部并没有运行 Redis 服务（Redis 运行在名为 `redis` 的独立容器中），导致应用抛出 `RedisConnectionFailureException` 或在日志中显示连接超时。

### 2. 根本原因分析 (Root Cause Analysis)

#### A. 命名空间彻底迁移 (Namespace Migration)
在 **Spring Boot 4.x** 系列（包括你使用的 v4.0.5）中，Spring 对数据源配置进行了严格的标准化重构。
* **失效的路径**：`spring.redis.*`。这一在 2.x 和 3.x 早期版本中通用的配置路径，在 4.x 中已被正式废弃或不再作为自动配置类（`RedisAutoConfiguration`）的默认读取来源。
* **生效的路径**：**`spring.data.redis.*`**。
* **逻辑断层**：当你提供 `spring.redis.host` 时，4.x 的自动配置类无法在新的 `spring.data.redis` 路径下找到匹配项，因此它不会报错，而是直接采用了代码中的硬编码默认值——即 `localhost`。

#### B. 环境变量匹配失效 (Relaxed Binding Failure)
由于配置路径的改变，你之前在 `docker-compose` 中注入的环境变量 `SPRING_REDIS_HOST` 同样无法映射到新版的配置属性上。Spring Boot 找不到符合新规范的 `SPRING_DATA_REDIS_HOST`，导致环境变量注入失效。

#### C. “严格模式”的触发 (Strict Mode)
日志显示：`Multiple Spring Data modules found, entering strict repository configuration mode`。
这意味着由于你同时引入了多种数据模块（如 MyBatis-Plus 和 Redis），Spring Boot 4.0.5 开启了**严格校验机制**。在这种模式下，框架对配置的规范性要求极高，任何不在正确命名空间（`spring.data`）下的配置都会被忽略，从而强制回退到默认的本地配置。

### 3. 错误模型模拟 (Model Simulation)



| 配置层级 | 你的输入 (旧规范) | Spring Boot 4.0.5 预期 (新规范) | 最终结果 |
| :--- | :--- | :--- | :--- |
| **Environment** | `SPRING_REDIS_HOST` | `SPRING_DATA_REDIS_HOST` | **忽略** |
| **Application YML** | `spring.redis.host` | `spring.data.redis.host` | **忽略** |
| **Default Value** | (无) | `localhost` | **生效** |

### 4. 最终解决方案 (Resolution)

为了修复此问题，必须完成从“业务命名空间”到“数据命名空间”的跨越：

1.  **配置文件重构**：将 `application-docker.yml` 中的 `spring.redis` 节点提升并嵌套至 `spring.data.redis`。
2.  **启动命令对齐**：修改 `docker-compose` 的 `command` 启动参数，将 `--spring.redis.host` 显式改为 `--spring.data.redis.host`。
3.  **环境变量对齐**：在 Compose 的 `environment` 列表中，同步增加 `SPRING_DATA_REDIS_HOST` 参数，以确保最高优先级的覆盖。

### 5. 经验教训 (Lessons Learned)
* **前瞻版本的代价**：使用 Spring Boot 4.0.5 这种前沿版本时，官方文档对 `spring.data` 结构的强制要求是最大的隐形坑。
* **日志敏感度**：`Multiple Spring Data modules found` 是一个预警信号，暗示了配置路径可能存在兼容性问题。
* **默认值陷阱**：在 Spring 生态中，“没报错但连了 localhost”通常意味着你的自定义配置根本没有被框架识别。

---
这次错误是典型的**架构升级导致的配置失焦**，由于 MySQL 的报错在前，掩盖了 Redis 配置已失效的事实。现在的综合修正方案已经彻底切断了它回退到 `localhost` 的路径。 🛡️💡

针对本次 Redis 连接故障，以下是基于你所使用的 **Spring Boot 4.0.5** 技术栈、容器化部署环境以及运行日志进行的深度技术总结。

---

## 🛑 故障核心：Spring Boot 4.x 配置路径迁移陷阱

本次 Redis 连接失败的根本原因并非网络不通或 Redis 服务宕机，而是由于 **Spring Boot 4.0.5 彻底废弃了旧版本的配置路径**，导致应用在 Docker 环境下产生“配置静默失效”并回退到默认值的连锁反应。

### 1. 配置路径的“代际断层”
在 Spring Boot 2.x 和 3.x 早期版本中，Redis 的配置通常位于 `spring.redis.*` 路径下。但在你使用的 **Spring Boot 4.0.5**（基于 Spring Framework 7.0）中，自动配置逻辑已全面向 **Spring Data 命名空间**对齐。

* **失效配置：** `spring.redis.host: redis`
* **预期配置：** `spring.data.redis.host: redis`



由于你的 `application-docker.yml` 或 `docker-compose.yml` 环境参数中仍在使用 `SPRING_REDIS_HOST`，Spring Boot 4.x 的 `RedisAutoConfiguration` 类在初始化时无法在 `spring.data.redis` 路径下探测到任何有效值。

### 2. “严格模式”触发的默认回退机制
你的日志中出现了一行关键提示：
> `Multiple Spring Data modules found, entering strict repository configuration mode`

这意味着项目中同时引入了多种数据源支持（如 MySQL 和 Redis）。在 **严格配置模式（Strict Mode）** 下，Spring Boot 4.x 对配置的准确性要求极高：
* 当它发现没有 `spring.data.redis.host` 的明确定义时，不会报错停止，而是选择**隐式回退（Silent Fallback）**。
* 它翻开了 `RedisProperties` 类的源码默认值，其中硬编码的 Host 为 `localhost`，Port 为 `6379`。

### 3. 容器环境下的“孤岛效应”
在 Docker 内部，`localhost` 指向的是 **应用容器本身**，而非运行 Redis 的那个容器。
* 应用尝试访问 `localhost:6379`。
* 由于应用容器内部并没有运行 Redis 进程，内核直接返回 `Connection refused`。
* 即便你的 Redis 容器（名为 `redis`）在同一个网络中运行得非常完美，应用也因为拿着“错误的地图”而永远无法找到它。

---

## 🛠️ 技术细节追踪：日志中的“无声告警”

虽然你的日志里没有直接喷出“配置错误”四个字，但以下细节揭示了 Redis 的亚健康状态：

* **Found 0 Redis repository interfaces**：
  这说明 Spring Data 扫描器虽然启动了，但因为配置路径的不匹配，它可能未能正确关联到你预期的自定义配置 Bean，或者处于一种高度解耦的低限度运行状态。
* **System Environment 优先级失效**：
  在你的初始 `docker-compose` 中，你通过环境变量传递了 `SPRING_REDIS_HOST`。但在 Spring Boot 的“松散绑定（Relaxed Binding）”逻辑中，虽然环境变量优先级高，但如果**Key 的名称（Path）根本没对上**，优先级再高也只是在一个不存在的频道里喊话。

---

## 📝 最终教训总结

| 故障维度 | 表现形式 | 技术根源 |
| :--- | :--- | :--- |
| **配置识别** | `spring.redis` 无法注入 | Spring Boot 4.x 强制要求使用 `spring.data.redis` 命名空间。 |
| **网络表现** | 连接 `localhost` 拒绝 | 环境变量 Key 错误导致自动配置回退到代码硬编码的默认值。 |
| **依赖冲突** | `Multiple Spring Data modules` | 开启了严格配置模式，任何非标准的配置路径都会被静默忽略。 |

**一句话总结：** 由于技术栈版本过于前卫（Spring Boot 4.x），项目在享受最新特性（Java 25）的同时，撞上了框架为了结构整洁而进行的**配置大清洗**。在 4.x 时代，所有的 Redis 外部注入必须严丝合缝地贴合 `spring.data.redis` 路径，否则应用就会由于“找不到配置”而固执地尝试连接并不存在的 `localhost`。 🛡️💻