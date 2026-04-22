好的，我来**逐行讲解**步骤9的审计日志代码，这是AOP的另一个经典应用场景。

---

## 一、审计日志整体架构

### 1.1 审计日志是什么？

**审计日志** = 记录谁、在什么时候、从哪个IP、对什么资源、做了什么操作、结果如何、花了多长时间。

**企业为什么需要审计日志？**
- **安全合规**：等保三级要求所有操作可追溯
- **问题排查**：出问题时知道谁干了什么
- **安全审计**：发现异常操作（如凌晨3点有人删库）
- **纠纷定责**：用户说"我没干"，日志证明是他干的

### 1.2 审计日志的完整流程

```
用户请求
    ↓
@AuditLog 注解标记
    ↓
AOP 切面拦截
    ↓
@Before: 记录开始时间、用户信息、IP、请求参数
    ↓
执行业务方法
    ↓
@AfterReturning: 成功，记录耗时
    或
@AfterThrowing: 失败，记录异常信息
    ↓
@Async 异步写入数据库（不阻塞主业务）
    ↓
返回响应给用户
```

---

## 二、注解定义讲解

### 2.1 @AuditLog 注解

```java
@Target({ElementType.METHOD})           // 只能用在方法上
@Retention(RetentionPolicy.RUNTIME)     // 运行时保留
@Documented
public @interface AuditLog {

    String value();                     // 操作名称

    boolean recordParams() default true;  // 是否记录参数

    boolean recordResult() default false; // 是否记录返回值（通常不记录，因为可能很大）
}
```

**使用示例**：
```java
@AuditLog("删除用户")           // 操作名称 = "删除用户"
@AuditLog(value = "登录", recordParams = false)  // 不记录参数（密码敏感）
```

### 2.2 为什么 recordResult 默认 false？

| 场景 | 返回值大小 | 是否记录 |
| :--- | :--- | :--- |
| 查询用户列表 | 可能几MB | ❌ 不记录 |
| 删除操作 | 只有成功/失败 | ✅ 可以记录 |
| 导出Excel | 几MB | ❌ 不记录 |

日志表如果存太大的返回值，会导致：
- 数据库空间爆炸
- 查询变慢
- 日志解析困难

---

## 三、实体类讲解

### 3.1 AuditLog 实体

```java
@TableName("audit_log")
public class AuditLog extends BaseEntity {
    private Long userId;        // 谁干的
    private String username;    // 用户名（冗余）
    private String operation;   // 干了什么（"删除用户"）
    private String method;      // 怎么干的（"DELETE /user/1"）
    private String params;      // 参数是什么（脱敏后）
    private String ip;          // 从哪来的
    private String result;      // 结果（SUCCESS/FAIL）
    private String errorMsg;    // 如果失败，为什么
    private Integer durationMs; // 花了多长时间
}
```

**为什么 username 要冗余存储？**

```sql
-- 如果不冗余，用户被删除后：
SELECT * FROM audit_log WHERE user_id = 123;
-- 查不到用户名，只知道"某个已删除的用户"干了什么

-- 冗余存储后：
SELECT * FROM audit_log WHERE username = '张三';
-- 即使张三被删了，日志仍然能查到
```

---

## 四、工具类讲解

### 4.1 WebUtils - 获取真实IP

```java
public static String getClientIp() {
    HttpServletRequest request = getCurrentRequest();

    // 1. 检查 X-Forwarded-For
    String ip = request.getHeader("X-Forwarded-For");
    if (ip != null && !ip.isEmpty() && !"unknown".equalsIgnoreCase(ip)) {
        int index = ip.indexOf(",");
        if (index != -1) {
            return ip.substring(0, index);  // 多级代理时，第一个是真实IP
        }
        return ip;
    }

    // 2. 其他代理头...
    // 3. 默认取 remoteAddr
    return request.getRemoteAddr();
}
```

**为什么需要这么复杂？**

```
用户电脑 (真实IP: 1.2.3.4)
    ↓
Nginx 代理 (添加 X-Forwarded-For: 1.2.3.4)
    ↓
负载均衡器
    ↓
应用服务器 (request.getRemoteAddr() 得到的是负载均衡器IP)
```

如果不解析 `X-Forwarded-For`，你永远只能看到代理服务器的IP，看不到真实用户IP。

### 4.2 SensitiveDataUtils - 参数脱敏

```java
private static final Pattern SENSITIVE_FIELDS = Pattern.compile(
    "(?i)(password|pwd|token|secret|authorization|access_token|refresh_token)"
);
```

**(?i) 含义**：不区分大小写
- 匹配 `password`、`Password`、`PASSWORD` 都行

**脱敏示例**：
```json
// 原始参数
{"username": "admin", "password": "123456", "phone": "13812345678"}

// 脱敏后
{"username": "admin", "password": "***", "phone": "138****5678"}
```

**为什么手机号要保留前3后4？**
- 可以区分不同用户（138****5678 和 139****1234 不同）
- 但又不能完整看到手机号（保护隐私）
- 这是业界标准做法

---

## 五、ThreadLocal 讲解

### 5.1 为什么需要 ThreadLocal？

```java
private final ThreadLocal<Long> startTimeThreadLocal = new ThreadLocal<>();
private final ThreadLocal<AuditLog> auditLogThreadLocal = new ThreadLocal<>();
```

**问题**：一个请求从进入AOP到结束，要经过三个方法：

```
doBefore() → doAfterReturning() → saveAuditLog()
```

如何在这三个方法之间传递数据？

**错误做法**：用成员变量
```java
private Long startTime;  // ❌ 多线程会互相覆盖

// 线程A: doBefore() 设置 startTime = 100
// 线程B: doBefore() 设置 startTime = 200  ← 覆盖了线程A的值
// 线程A: doAfterReturning() 读取 startTime = 200  ← 读错了
```

**正确做法**：用 ThreadLocal
```java
private ThreadLocal<Long> startTimeThreadLocal = new ThreadLocal<>();

// 线程A: startTimeThreadLocal.set(100)  ← 存在线程A的私有空间
// 线程B: startTimeThreadLocal.set(200)  ← 存在线程B的私有空间
// 线程A: startTimeThreadLocal.get() = 100  ← 互不干扰
```

### 5.2 ThreadLocal 工作原理

```
ThreadLocal 内部结构：

Thread-1 (请求A)
    ├── threadLocalMap
    │     ├── startTimeThreadLocal → 100
    │     └── auditLogThreadLocal → AuditLog对象A

Thread-2 (请求B)
    ├── threadLocalMap
    │     ├── startTimeThreadLocal → 200
    │     └── auditLogThreadLocal → AuditLog对象B
```

每个线程有自己的 `ThreadLocalMap`，键是 ThreadLocal 对象，值是存储的数据。

### 5.3 为什么要在 finally 中 remove？

```java
finally {
    startTimeThreadLocal.remove();
    auditLogThreadLocal.remove();
}
```

**Tomcat 线程池复用问题**：

```
请求A处理完 → 线程1 放回池子（ThreadLocal 中的数据还在！）
请求B来了 → 复用线程1
    ↓
请求B 调用 startTimeThreadLocal.get() → 拿到了请求A的数据！← BUG！
```

**remove() 的作用**：清除当前线程的 ThreadLocal 数据，防止下次复用线程时读到旧数据。

---

## 六、切面类讲解

### 6.1 切点定义

```java
@Pointcut("@annotation(auditLog)")
public void auditLogPointcut(AuditLog auditLog) {
}
```

**作用**：定义一个切点，匹配所有标注了 `@AuditLog` 注解的方法，并把注解对象作为参数传进来。

### 6.2 @Before 前置通知

```java
@Before(value = "auditLogPointcut(auditLog)", argNames = "joinPoint,auditLog")
public void doBefore(JoinPoint joinPoint, AuditLog auditLog) {
    // 1. 记录开始时间
    startTimeThreadLocal.set(System.currentTimeMillis());

    // 2. 创建日志对象
    AuditLog logEntity = new AuditLog();

    // 3. 获取当前登录用户
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    if (authentication != null && authentication.isAuthenticated()) {
        // authentication.getCredentials() 存储的是 userId（见JwtFilter）
        logEntity.setUserId((Long) authentication.getCredentials());
        logEntity.setUsername(((User) authentication.getPrincipal()).getUsername());
    }

    // 4. 获取请求信息
    HttpServletRequest request = WebUtils.getCurrentRequest();
    logEntity.setMethod(request.getMethod() + " " + request.getRequestURI());
    logEntity.setIp(WebUtils.getClientIp());

    // 5. 记录参数（脱敏）
    if (auditLog.recordParams()) {
        Object[] args = joinPoint.getArgs();
        // 排除 HttpServletRequest 等框架对象
        for (Object arg : args) {
            if (arg != null && !(arg instanceof HttpServletRequest)) {
                logEntity.setParams(SensitiveDataUtils.desensitize(arg));
                break;
            }
        }
    }

    auditLogThreadLocal.set(logEntity);
}
```

**关键点**：

| 代码 | 作用 |
| :--- | :--- |
| `startTimeThreadLocal.set()` | 存储开始时间，供后续计算耗时 |
| `authentication.getCredentials()` | 获取 userId（JwtFilter 中存入的） |
| `joinPoint.getArgs()` | 获取方法参数，用于记录 |
| `auditLogThreadLocal.set()` | 存储日志对象，供后续方法使用 |

### 6.3 @AfterReturning 后置通知

```java
@AfterReturning(pointcut = "auditLogPointcut(auditLog)",
                argNames = "joinPoint,auditLog,returning",
                returning = "returning")
public void doAfterReturning(JoinPoint joinPoint, AuditLog auditLog, Object returning) {
    saveAuditLog();  // 异步保存
}
```

**returning 参数**：可以拿到方法的返回值，用于记录。但默认不记录返回值（太大）。

### 6.4 @AfterThrowing 异常通知

```java
@AfterThrowing(pointcut = "auditLogPointcut(auditLog)",
               argNames = "joinPoint,auditLog,e",
               throwing = "e")
public void doAfterThrowing(JoinPoint joinPoint, AuditLog auditLog, Exception e) {
    AuditLog logEntity = auditLogThreadLocal.get();
    if (logEntity != null) {
        logEntity.setResult("FAIL");
        // 限制异常信息长度，防止撑爆数据库
        String errorMsg = e.getMessage();
        if (errorMsg != null && errorMsg.length() > 500) {
            errorMsg = errorMsg.substring(0, 500);
        }
        logEntity.setErrorMsg(errorMsg);
    }
    saveAuditLog();
}
```

**为什么限制异常信息长度？**
- 数据库字段 `error_msg VARCHAR(500)`
- 某些异常堆栈可能几千字符
- 截断后插入，防止 SQL 报错

### 6.5 @Async 异步保存

```java
@Async("auditLogExecutor")
public void saveAuditLog() {
    try {
        AuditLog logEntity = auditLogThreadLocal.get();
        // 计算耗时
        Long startTime = startTimeThreadLocal.get();
        if (startTime != null) {
            logEntity.setDurationMs((int)(System.currentTimeMillis() - startTime));
        }
        auditLogMapper.insert(logEntity);
    } catch (Exception e) {
        log.error("审计日志保存失败: {}", e.getMessage());
        // 不抛异常，不影响主业务
    } finally {
        startTimeThreadLocal.remove();
        auditLogThreadLocal.remove();
    }
}
```

**为什么用 @Async？**
- 主业务不需要等待日志写完
- 即使日志数据库挂了，主业务也不受影响

**为什么 catch 异常后只打印日志？**
- 日志记录失败不应该导致用户操作失败
- 用户删除了用户，日志没记上，但删除操作应该成功

---

## 七、线程池配置讲解

```java
@Bean("auditLogExecutor")
public Executor auditLogExecutor() {
    ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
    executor.setCorePoolSize(2);      // 核心线程数
    executor.setMaxPoolSize(5);       // 最大线程数
    executor.setQueueCapacity(100);   // 队列容量
    executor.setThreadNamePrefix("audit-log-");
    executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
    return executor;
}
```

**参数说明**：

| 参数 | 值 | 含义 |
| :--- | :--- | :--- |
| corePoolSize | 2 | 即使空闲也保留2个线程 |
| maxPoolSize | 5 | 最多创建5个线程 |
| queueCapacity | 100 | 最多排队100个任务 |

**任务处理流程**：
```
任务到达
    ↓
当前线程数 < corePoolSize? → 是 → 创建新线程执行
    ↓ 否
队列未满? → 是 → 放入队列等待
    ↓ 否
当前线程数 < maxPoolSize? → 是 → 创建新线程执行
    ↓ 否
执行拒绝策略（CallerRunsPolicy：由调用线程执行）
```

**CallerRunsPolicy 的含义**：
- 队列满了，线程数也到上限了
- 新任务由调用方线程（主业务线程）自己执行
- 相当于降级：日志写不进去就同步写，保证不丢失

---

## 八、完整执行流程示例

### 示例：用户删除操作

```java
@DeleteMapping("/{id}")
@PreAuthorize("hasAuthority('user:delete')")
@AuditLog("删除用户")
public Result<Void> delete(@PathVariable Long id) {
    userService.delete(id);
    return Result.success();
}
```

**执行流程**：

```
1. 请求进入 DELETE /user/1
    ↓
2. JwtFilter：认证通过，存入 SecurityContext
    ↓
3. AOP 拦截到 @AuditLog("删除用户")
    ↓
4. doBefore() 执行：
   - 记录开始时间：startTime = T1
   - 创建 logEntity
   - 从 SecurityContext 获取 userId=1, username=admin
   - 获取请求信息：method=DELETE /user/1, ip=127.0.0.1
   - 获取参数：id=1
   - logEntity.setResult("SUCCESS")（默认）
   - 存入 ThreadLocal
    ↓
5. 执行业务方法 delete()
    ↓
6. doAfterReturning() 执行：
   - 调用 saveAuditLog()
    ↓
7. saveAuditLog()（异步线程执行）：
   - 计算耗时：duration = T2 - T1
   - 插入数据库
   - 清理 ThreadLocal
    ↓
8. 返回响应给用户
```

**数据库最终记录**：
```
id=100, user_id=1, username=admin, operation=删除用户,
method=DELETE /user/1, params={"id":1}, ip=127.0.0.1,
result=SUCCESS, duration_ms=25, create_time=2024-01-01 10:00:00
```

---

## 九、常见问题解答

### Q1：ThreadLocal 会造成内存泄漏吗？

**会**，如果忘记 remove()。

**原因**：
- ThreadLocalMap 的 key 是弱引用，但 value 是强引用
- 线程被回收时，ThreadLocalMap 也会被回收
- 但 Tomcat 线程池会复用线程，线程不会回收
- 如果一直不 remove()，value 会一直存在，导致内存泄漏

**解决方案**：在 finally 中 remove()

### Q2：为什么不用 @Around 而用 @Before + @AfterReturning？

| 方案 | 优点 | 缺点 |
| :--- | :--- | :--- |
| @Around | 一个方法搞定所有 | 必须手动调用 proceed()，容易出错 |
| @Before + @AfterReturning | 职责分离，清晰 | 需要 ThreadLocal 传递数据 |

两者都可以，企业项目更倾向于分开写，因为更清晰。

### Q3：参数脱敏会不会影响性能？

```java
String json = objectMapper.writeValueAsString(params);  // 序列化
```

- 每次请求都做一次 JSON 序列化
- 但日志是异步的，不阻塞主业务
- 可以接受

### Q4：日志表数据量太大怎么办？

| 方案 | 说明 |
| :--- | :--- |
| 分表 | 按月分表：audit_log_202401, audit_log_202402 |
| 归档 | 3个月前的数据移到归档表 |
| 清理 | 只保留90天，定期删除 |

---

## 下一步

确认理解了审计日志后，告诉我，我继续给你**步骤10：安全加固（限流 + 防重放）**的代码。