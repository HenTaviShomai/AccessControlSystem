好的，我来**逐行讲解**步骤10的安全加固代码，这是生产环境必备的防护措施。

---

## 一、为什么需要安全加固？

### 1.1 常见的安全威胁

| 攻击类型 | 描述 | 后果 |
| :--- | :--- | :--- |
| **暴力破解** | 不断尝试用户名/密码组合 | 账号被盗 |
| **DDoS攻击** | 大量请求耗尽服务器资源 | 服务瘫痪 |
| **重复提交** | 用户快速点击按钮多次 | 重复扣款、重复创建订单 |
| **重放攻击** | 拦截请求后重复发送 | 恶意操作 |

### 1.2 本步骤解决的三大问题

```
┌─────────────────────────────────────────────────────────────┐
│                     安全防护体系                             │
├─────────────────────────────────────────────────────────────┤
│  1. 限流（RateLimit）    → 防止DDoS和暴力破解                 │
│  2. 防重复提交（NoRepeat）→ 防止用户快速点击                 │
│  3. 登录失败锁定         → 防止密码暴力破解                   │
└─────────────────────────────────────────────────────────────┘
```

---

## 二、限流注解和切面讲解

### 2.1 @RateLimit 注解

```java
@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface RateLimit {
    String key();                    // 限流的key
    double permitsPerSecond() default 10.0;  // 每秒令牌数
    int timeout() default 1;         // 获取令牌超时时间
    TimeUnit timeUnit() default TimeUnit.SECONDS;  // 时间单位
}
```

**使用示例**：
```java
@RateLimit(key = "login", permitsPerSecond = 5)
public Result<LoginResponse> login(...) { ... }
```

**参数含义**：
- `key = "login"`：区分不同接口（登录接口和查询接口分开限流）
- `permitsPerSecond = 5`：每秒最多处理5个请求
- `timeout = 1`：等待1秒获取令牌，拿不到就拒绝

### 2.2 RateLimiter 的两种获取方式

```java
// 方式1：阻塞等待（不推荐）
rateLimiter.acquire();  // 如果没有令牌，会一直等下去

// 方式2：非阻塞（推荐）
boolean acquired = rateLimiter.tryAcquire(timeout, timeUnit);  // 等待指定时间
```

**为什么用 tryAcquire 而不是 acquire？**
- `acquire` 会让请求排队，适合异步处理
- 同步接口用 `tryAcquire`，超时直接拒绝，用户体验更好

### 2.3 限流Key的设计

```java
String limitKey = method.getDeclaringClass().getSimpleName() + ":" 
                + method.getName() + ":" 
                + rateLimit.key() + ":" 
                + clientIp;
```

**示例**：
```
AuthController:login:login:192.168.1.100
```

**为什么包含IP？**
- 不同IP应该独立限流（防止一个IP把所有人的配额用完）
- 同一个IP的请求共享限流配额

### 2.4 ConcurrentHashMap 缓存 RateLimiter

```java
private final ConcurrentHashMap<String, RateLimiter> rateLimiterMap = new ConcurrentHashMap<>();

RateLimiter rateLimiter = rateLimiterMap.computeIfAbsent(limitKey,
    k -> RateLimiter.create(rateLimit.permitsPerSecond()));
```

**为什么用 ConcurrentHashMap？**
- 多个请求同时访问时，保证线程安全
- `computeIfAbsent` 是原子操作，不会重复创建

**流程图**：
```
请求1进入 → limitKey="AuthController:login:login:1.2.3.4"
          → map中不存在 → 创建RateLimiter(5) → 存入map
请求2进入 → limitKey相同 → map中已存在 → 直接使用
```

---

## 三、防重复提交切面讲解

### 3.1 @NoRepeatSubmit 注解

```java
@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface NoRepeatSubmit {
    long duration() default 3;      // 锁定时间（秒）
    TimeUnit timeUnit() default TimeUnit.SECONDS;
}
```

**使用示例**：
```java
@NoRepeatSubmit(duration = 3)  // 3秒内不能重复提交
public Result<Void> add(@RequestBody UserRequest request) { ... }
```

### 3.2 @Around 环绕通知

```java
@Around("@annotation(com.AccessControlSystem.annotation.NoRepeatSubmit)")
public Object handleNoRepeatSubmit(ProceedingJoinPoint joinPoint) throws Throwable {
    // 1. 前置处理（检查是否重复）
    // 2. 执行目标方法
    // 3. 后置处理
}
```

**@Before 和 @Around 的区别**：

| 通知类型 | 能否控制方法执行 | 能否修改返回值 | 适用场景 |
| :--- | :--- | :--- | :--- |
| `@Before` | 不能 | 不能 | 日志、权限校验 |
| `@Around` | 能 | 能 | 缓存、防重复、性能监控 |

**@Around 的典型结构**：
```java
@Around("...")
public Object around(ProceedingJoinPoint joinPoint) throws Throwable {
    // 前置逻辑
    log.info("方法执行前");
    
    // 执行目标方法
    Object result = joinPoint.proceed();
    
    // 后置逻辑
    log.info("方法执行后");
    
    return result;
}
```

### 3.3 防重复Key的设计

```java
String repeatKey = String.format("%s%s:%s:%s:%s:%s",
    RedisConstants.REPEAT_SUBMIT_KEY,  // "repeat:submit:"
    userId,           // 用户ID
    className,        // 类名
    methodName,       // 方法名
    paramsHash,       // 参数Hash
    duration);        // 锁定时间
```

**示例**：
```
repeat:submit:1:UserController:add:-123456789:3
         ↑      ↑        ↑      ↑     ↑       ↑
       前缀   用户ID   类名   方法名 参数Hash 锁定时长
```

**为什么包含参数Hash？**
- 同一个用户，3秒内提交不同的数据，应该允许
- 只有完全相同的参数才拦截

**参数Hash的计算**：
```java
String paramStr = SensitiveDataUtils.desensitize(param);
return String.valueOf(paramStr.hashCode());
```

### 3.4 Redis SETNX 命令

```java
Boolean success = redisTemplate.opsForValue()
    .setIfAbsent(repeatKey, "1", duration, timeUnit);
```

**SETNX = SET if Not eXists（不存在则设置）**

| 情况 | 返回值 | 说明 |
| :--- | :--- | :--- |
| Key不存在 | `true` | 设置成功，第一次请求 |
| Key已存在 | `false` | 设置失败，重复请求 |

**流程图**：
```
用户第一次提交：
  SETNX key value EX 3 → 成功（返回true）
  → 执行业务逻辑
  → 3秒后key自动过期

用户在3秒内第二次提交：
  SETNX key value EX 3 → 失败（返回false）
  → 抛出"请勿重复提交"异常
  → 不执行业务逻辑
```

---

## 四、登录失败限制讲解

### 4.1 为什么需要登录失败限制？

**没有限制的情况**：
```
攻击者脚本：
for i in {1..1000000}; do
    curl -X POST /auth/login -d '{"username":"admin","password":"guess'$i'"}'
done
```

**后果**：
- 可以无限次尝试密码
- 弱密码很容易被暴力破解

**有限制的情况**：
```
连续失败5次 → 锁定15分钟 → 攻击者无法继续尝试
```

### 4.2 登录失败计数流程

```java
public void loginFailed(String username) {
    String failKey = "auth:login:fail:" + username;
    String attemptsStr = redisTemplate.opsForValue().get(failKey);
    int attempts = attemptsStr == null ? 0 : Integer.parseInt(attemptsStr);
    
    attempts++;
    
    if (attempts >= MAX_ATTEMPTS) {  // 5次
        // 锁定账号
        String lockKey = "auth:login:lock:" + username;
        redisTemplate.opsForValue().set(lockKey, "1", 15, TimeUnit.MINUTES);
        redisTemplate.delete(failKey);
    } else {
        // 记录失败次数，1小时后过期
        redisTemplate.opsForValue().set(failKey, String.valueOf(attempts), 1, TimeUnit.HOURS);
    }
}
```

**Redis数据变化示例**：

| 操作 | failKey的值 | lockKey的值 |
| :--- | :--- | :--- |
| 第1次失败 | `1` | 不存在 |
| 第2次失败 | `2` | 不存在 |
| 第3次失败 | `3` | 不存在 |
| 第4次失败 | `4` | 不存在 |
| 第5次失败 | 删除 | `1`（过期时间15分钟） |

### 4.3 检查账号是否被锁定

```java
public boolean isLocked(String username) {
    String lockKey = "auth:login:lock:" + username;
    return Boolean.TRUE.equals(redisTemplate.hasKey(lockKey));
}
```

**登录时的完整流程**：
```java
// 1. 先检查是否锁定
if (loginAttemptService.isLocked(username)) {
    throw new BusinessException(ErrorCode.USER_LOCKED);
}

// 2. 查询用户
User user = userMapper.selectByUsername(username);
if (user == null) {
    loginAttemptService.loginFailed(username);  // 记录失败
    throw new BusinessException(ErrorCode.USER_NOT_FOUND);
}

// 3. 校验密码
if (!passwordEncoder.matches(password, user.getPassword())) {
    loginAttemptService.loginFailed(username);  // 记录失败
    throw new BusinessException(ErrorCode.PASSWORD_ERROR);
}

// 4. 登录成功，清除失败记录
loginAttemptService.loginSucceeded(username);
```

---

## 五、三个切面的执行顺序

当多个切面同时作用在一个方法上时，执行顺序很重要。

```java
@RateLimit(key = "add", permitsPerSecond = 10)
@NoRepeatSubmit(duration = 3)
@AuditLog("新增用户")
public Result<Void> add(...) { ... }
```

**执行顺序**：
```
请求进入
    ↓
1. RateLimitAspect（先限流）
    ├── 限流通过 → 继续
    └── 限流拒绝 → 直接返回错误
    ↓
2. NoRepeatSubmitAspect（防重复）
    ├── 第一次请求 → 继续
    └── 重复请求 → 直接返回错误
    ↓
3. AuditLogAspect（记录日志）
    ├── @Before：记录开始时间
    ├── 执行目标方法
    └── @AfterReturning：异步保存日志
    ↓
返回响应
```

**为什么限流在最外层？**
- 限流是最粗粒度的防护
- 先把恶意流量挡在外面，减少后续处理开销

---

## 六、Guava RateLimiter 的深入讲解

### 6.1 依赖引入

```xml
<dependency>
    <groupId>com.google.guava</groupId>
    <artifactId>guava</artifactId>
    <version>32.1.3-jre</version>
</dependency>
```

### 6.2 令牌桶算法图解

```
        ┌─────────────────────────────────┐
        │          令牌桶                   │
        │  ⚪ ⚪ ⚪ ⚪ ⚪                    │
        │  容量 = 5                        │
        └─────────────────────────────────┘
                ↑           ↓
        以5个/秒的速度   每个请求消耗1个令牌
          放入令牌
                ↓           ↓
        ┌─────────────────────────────────┐
        │          请求队列                 │
        │  📨 📨 📨 📨 📨                  │
        └─────────────────────────────────┘
```

### 6.3 平滑突发限制

```java
RateLimiter limiter = RateLimiter.create(5.0);  // 平均5个/秒

// 瞬间发送10个请求
for (int i = 0; i < 10; i++) {
    System.out.println(limiter.acquire());  // 打印等待时间
}
```

**输出示例**：
```
0.0       // 第1个立即执行
0.0       // 第2个立即执行
0.0       // 第3个立即执行
0.0       // 第4个立即执行
0.0       // 第5个立即执行
0.2       // 第6个等待0.2秒
0.2       // 第7个等待0.2秒
0.2       // 第8个等待0.2秒
0.2       // 第9个等待0.2秒
0.2       // 第10个等待0.2秒
```

---

## 七、完整测试流程

### 7.1 测试限流

```bash
# 快速发送10次登录请求
for i in {1..10}; do
    time http POST localhost:8080/auth/login username=admin password=123456
done
```

**预期**：
- 前5次：正常返回token
- 后5次：返回 `{"code":4007,"msg":"请求过于频繁"}`

### 7.2 测试防重复

```bash
# 同时发送两个请求（用&实现并行）
http POST localhost:8080/user username=test99 password=123456 nickname=测试99 "Authorization: Bearer $TOKEN" &
http POST localhost:8080/user username=test99 password=123456 nickname=测试99 "Authorization: Bearer $TOKEN"
```

**预期**：
- 一个请求成功
- 另一个返回 `{"code":4008,"msg":"请勿重复提交"}`

### 7.3 测试登录锁定

```bash
# 连续5次错误密码
for i in {1..5}; do
    http POST localhost:8080/auth/login username=admin password=wrong$i
done

# 第6次正确密码
http POST localhost:8080/auth/login username=admin password=123456
```

**预期**：
- 前5次：返回"密码错误"
- 第6次：返回"账号已被锁定，请15分钟后重试"

---

## 八、常见问题解答

### Q1：为什么限流不用 Redis 而用 Guava？

| 方案 | 优点 | 缺点 |
| :--- | :--- | :--- |
| Guava（本地） | 快（无网络开销），实现简单 | 多实例不同步 |
| Redis（分布式） | 多实例共享 | 有网络延迟 |

**本项目选择**：单实例部署 + Guava
- 在校生项目，单实例足够
- 面试时可以说"后续可以升级为Redis分布式限流"

### Q2：防重复提交的 Key 为什么不包含 IP？

因为同一个用户可能在公司网络（共享IP），如果包含IP会导致不同用户被误拦截。

### Q3：登录失败锁定为什么用 Redis 不用数据库？

- 失败次数是临时状态，不需要持久化
- Redis 支持自动过期（1小时后清空计数）
- 读写性能比数据库高

### Q4：Guava RateLimiter 的 permitsPerSecond 应该设置多少？

| 接口类型 | 推荐值 | 说明 |
| :--- | :--- | :--- |
| 登录接口 | 5-10 | 防止暴力破解 |
| 查询接口 | 20-50 | 用户正常查询 |
| 导出接口 | 1-2 | 导出消耗资源大 |
| 写接口 | 10-20 | 防重复即可 |

---

## 下一步

确认理解了安全加固的代码后，告诉我，我继续给你**步骤11：单元测试**的代码。