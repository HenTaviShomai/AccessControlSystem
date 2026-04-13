好的，我来**逐行讲解**刚才补充的AOP权限校验代码，这是整个权限系统中最能体现"企业级"的部分。

---

## 一、为什么需要自定义权限注解 + AOP？

### 1.1 Spring Security 的 `@PreAuthorize` 有什么问题？

```java
// Spring Security 原生方式
@PreAuthorize("hasAuthority('user:list') and hasAuthority('user:add')")
```

| 问题 | 说明 |
| :--- | :--- |
| **字符串表达式** | 写错了编译器不报错，运行时才发现 |
| **IDE不支持** | 没有代码补全，没有跳转 |
| **重构困难** | 权限码改名后，需要全文搜索替换 |
| **复杂表达式难读** | `hasAuthority('a') and hasAuthority('b') or hasRole('c')` |

### 1.2 自定义注解的优势

```java
// 自定义注解方式
@RequirePermission(value = {"user:list", "user:add"}, logical = Logical.AND)
```

| 优势 | 说明 |
| :--- | :--- |
| **类型安全** | 编译期检查 |
| **IDE友好** | 有代码补全，可以跳转定义 |
| **可扩展** | 可以加其他功能（如记录日志、限流） |
| **可复用** | 定义一次，到处使用 |

### 1.3 AOP 在这里的角色

```
@RequirePermission 注解
        ↓
   AOP 切面拦截
        ↓
   提取注解中的权限要求
        ↓
   从 SecurityContext 获取用户权限
        ↓
   比对 → 通过则放行，不通过则抛异常
```

**AOP 让你可以在不修改业务代码的情况下，横切性地添加权限校验逻辑。**

---

## 二、自定义注解讲解

### 2.1 @RequirePermission 完整解析

```java
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface RequirePermission {
    
    String[] value();  // 需要的权限码数组
    
    Logical logical() default Logical.AND;  // 逻辑关系，默认AND
    
    enum Logical {
        AND, OR
    }
}
```

**逐行解释**：

| 注解/关键字 | 作用 | 说明 |
| :--- | :--- | :--- |
| `@Target` | 注解可以放在哪里 | `METHOD`=方法上，`TYPE`=类上 |
| `@Retention` | 注解保留到什么时候 | `RUNTIME`=运行时也保留，这样才能用反射读取 |
| `@Documented` | 是否生成到Javadoc | 让文档生成工具包含这个注解 |
| `String[] value()` | 注解的参数 | 使用方式：`@RequirePermission("user:list")` 或 `@RequirePermission({"user:list", "user:add"})` |
| `default` | 默认值 | `logical` 不写时默认为 `AND` |

### 2.2 使用示例

```java
// 单个权限
@RequirePermission("user:list")

// 多个权限（需要全部满足）
@RequirePermission(value = {"user:list", "user:add"}, logical = Logical.AND)

// 多个权限（满足任意一个即可）
@RequirePermission(value = {"user:list", "role:list"}, logical = Logical.OR)
```

---

## 三、AOP切面讲解

### 3.1 切面类结构

```java
@Slf4j                          // 日志
@Aspect                         // 告诉Spring这是一个切面类
@Component                      // 让Spring管理这个Bean
public class PermissionAspect {
    
    // 切点 + 通知
    @Before("@annotation(com.AccessControlSystem.annotation.RequirePermission)")
    public void checkPermission(JoinPoint joinPoint) {
        // 校验逻辑
    }
}
```

### 3.2 @Before 注解详解

```java
@Before("@annotation(com.AccessControlSystem.annotation.RequirePermission)")
```

| 组成部分 | 含义 |
| :--- | :--- |
| `@Before` | 前置通知，在方法执行**之前**执行 |
| `@annotation(...)` | 切点表达式，匹配所有标注了`RequirePermission`注解的方法 |

**其他类型的通知**：

| 通知类型 | 执行时机 | 适用场景 |
| :--- | :--- | :--- |
| `@Before` | 方法执行前 | 权限校验、参数校验 |
| `@After` | 方法执行后（无论是否异常） | 资源清理 |
| `@AfterReturning` | 方法正常返回后 | 记录成功日志 |
| `@AfterThrowing` | 方法抛异常后 | 记录异常日志 |
| `@Around` | 方法执行前后 | 性能监控、事务管理 |

### 3.3 获取方法上的注解

```java
MethodSignature signature = (MethodSignature) joinPoint.getSignature();
Method method = signature.getMethod();
RequirePermission requirePermission = method.getAnnotation(RequirePermission.class);
```

**为什么要这样写？**

```
joinPoint 包含了被拦截方法的所有信息
    ↓
getSignature() 获取方法签名
    ↓
转型为 MethodSignature（能拿到更多方法信息）
    ↓
getMethod() 获取 Method 对象
    ↓
getAnnotation() 获取方法上的 RequirePermission 注解
```

**完整流程图**：
```
@RequirePermission("user:list")
public Result deleteUser() { ... }
        ↓
    AOP 拦截
        ↓
joinPoint.getSignature() → "Result deleteUser()"
        ↓
signature.getMethod() → Method 对象
        ↓
method.getAnnotation() → RequirePermission 对象
        ↓
requirePermission.value() → ["user:list"]
requirePermission.logical() → AND
```

### 3.4 获取当前用户权限

```java
private Set<String> getCurrentUserPermissions() {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    if (authentication == null || !authentication.isAuthenticated()) {
        return Set.of();  // 未登录，返回空集合
    }
    
    Collection<? extends GrantedAuthority> authorities = authentication.getAuthorities();
    return authorities.stream()
            .map(GrantedAuthority::getAuthority)
            .collect(Collectors.toSet());
}
```

**SecurityContext 中的数据来源**：

```
JwtAuthenticationFilter 做的事：
    ↓
1. 解析 JWT，得到 userId
2. 从 Redis 查询权限：["user:list", "user:add", "user:delete"]
3. 转换成 SimpleGrantedAuthority 对象
4. 存入 SecurityContextHolder
    ↓
getCurrentUserPermissions() 从这里读取
```

**数据结构示例**：
```java
// SecurityContext 中存储的权限
authentication.getAuthorities() = [
    SimpleGrantedAuthority("user:list"),
    SimpleGrantedAuthority("user:add"),
    SimpleGrantedAuthority("user:delete")
]

// 转换为 Set<String>
userPermissions = ["user:list", "user:add", "user:delete"]
```

### 3.5 权限校验逻辑

```java
private boolean check(Set<String> userPermissions, String[] required, RequirePermission.Logical logical) {
    if (required.length == 0) {
        return true;  // 没有要求任何权限，直接通过
    }
    
    if (logical == RequirePermission.Logical.AND) {
        // AND：需要拥有所有权限
        return Arrays.stream(required).allMatch(userPermissions::contains);
    } else {
        // OR：只需要拥有任一权限
        return Arrays.stream(required).anyMatch(userPermissions::contains);
    }
}
```

**AND 逻辑详解**：

```java
Arrays.stream(required).allMatch(userPermissions::contains)
```

| 步骤 | 说明 |
| :--- | :--- |
| `Arrays.stream(required)` | 将数组 `["a","b","c"]` 转为流 |
| `.allMatch(...)` | 每个元素都要满足条件 |
| `userPermissions::contains` | 检查用户权限集合是否包含该权限 |

**示例**：
```java
required = ["user:list", "user:add"]
userPermissions = ["user:list", "user:add", "user:delete"]

// allMatch 执行过程：
"user:list" → userPermissions.contains("user:list") → true
"user:add"  → userPermissions.contains("user:add")  → true
// 全部 true → 返回 true
```

**OR 逻辑详解**：

```java
Arrays.stream(required).anyMatch(userPermissions::contains)
```

| 步骤 | 说明 |
| :--- | :--- |
| `.anyMatch(...)` | 只要有一个元素满足条件就返回 true |

**示例**：
```java
required = ["user:list", "role:list"]
userPermissions = ["user:add", "user:delete"]

// anyMatch 执行过程：
"user:list" → false
"role:list" → false
// 没有一个 true → 返回 false
```

### 3.6 权限不足时的处理

```java
if (!hasPermission) {
    log.warn("权限不足: 用户权限={}, 需要权限={}, 逻辑={}", 
             userPermissions, Arrays.toString(requiredPermissions), logical);
    throw new BusinessException(ErrorCode.NO_PERMISSION);
}
```

**流程**：
```
抛出 BusinessException
        ↓
GlobalExceptionHandler 捕获
        ↓
返回 Result.error(3001, "无操作权限")
        ↓
前端收到 403
```

---

## 四、AOP配置讲解

### 4.1 @EnableAspectJAutoProxy

```java
@Configuration
@EnableAspectJAutoProxy(proxyTargetClass = true)
public class AopConfig {
}
```

| 参数 | 作用 |
| :--- | :--- |
| `proxyTargetClass = true` | 使用CGLIB代理（基于类）而不是JDK代理（基于接口） |

**为什么设置 `proxyTargetClass = true`？**

| 代理方式 | 原理 | 要求 | 推荐 |
| :--- | :--- | :--- | :--- |
| JDK动态代理 | 基于接口 | 目标类必须实现接口 | 不推荐 |
| CGLIB代理 | 基于子类 | 目标类不能是final | ✅ 推荐 |

**示例**：
```java
// UserController 没有实现接口
@RestController
public class UserController {
    // JDK代理无法代理这个类（因为没有接口）
    // CGLIB代理可以（生成子类）
}
```

---

## 五、完整执行流程图

```
请求: DELETE /user/1
Headers: Authorization: Bearer xxx
    ↓
┌─────────────────────────────────────────────────────────────┐
│ 1. JwtAuthenticationFilter                                  │
│    - 解析Token                                               │
│    - 从Redis加载权限: ["user:delete", "user:list"]           │
│    - 存入SecurityContext                                     │
└─────────────────────────────────────────────────────────────┘
    ↓
┌─────────────────────────────────────────────────────────────┐
│ 2. DispatcherServlet 路由                                   │
│    - 找到 UserController.delete() 方法                       │
│    - 发现方法上有 @RequirePermission("user:delete")          │
└─────────────────────────────────────────────────────────────┘
    ↓
┌─────────────────────────────────────────────────────────────┐
│ 3. PermissionAspect 切面                                    │
│    - @Before 通知触发                                        │
│    - 读取注解: required = ["user:delete"], logical = AND     │
│    - 从SecurityContext获取用户权限: ["user:delete", "user:list"] │
│    - 校验: "user:delete" in ["user:delete", "user:list"] → true │
│    - 通过校验，继续执行                                       │
└─────────────────────────────────────────────────────────────┘
    ↓
┌─────────────────────────────────────────────────────────────┐
│ 4. UserController.delete() 执行                             │
│    - userService.delete(1)                                  │
│    - 返回 Result.success()                                  │
└─────────────────────────────────────────────────────────────┘
    ↓
返回: {"code":0, "msg":"成功"}
```

---

## 六、两种权限校验方式对比

| 维度 | @PreAuthorize | @RequirePermission + AOP |
| :--- | :--- | :--- |
| **依赖** | Spring Security | 自己实现 |
| **灵活性** | 高（支持复杂表达式） | 中（需要自己扩展） |
| **类型安全** | 低（字符串表达式） | 高（编译期检查） |
| **IDE支持** | 无 | 有代码补全 |
| **可扩展性** | 低（需要配置） | 高（直接改代码） |
| **学习成本** | 需要学SpEL表达式 | 普通Java |
| **企业使用** | 中小项目 | 大项目（封装自己的权限框架） |

**建议**：
- 简单项目：直接用 `@PreAuthorize`
- 企业项目：自定义注解 + AOP（更可控）

---

## 七、常见问题解答

### Q1：AOP 为什么没有执行？

**可能原因**：
1. 忘记加 `@Aspect` 注解
2. 忘记加 `@Component` 注解
3. 忘记加 `@EnableAspectJAutoProxy`
4. 方法被 `private` 修饰（AOP只能代理public方法）
5. 方法被同一个类内部调用（内部调用不走代理）

### Q2：`JoinPoint` 是什么？

```java
public void checkPermission(JoinPoint joinPoint)
```

`JoinPoint` 是被拦截方法的**上下文对象**，包含：
- `getSignature()`：方法签名
- `getArgs()`：方法参数
- `getTarget()`：目标对象
- `getThis()`：代理对象

### Q3：`@annotation` 切点表达式还能怎么写？

| 表达式 | 匹配范围 |
| :--- | :--- |
| `@annotation(RequirePermission)` | 方法上有该注解 |
| `within(com.example..*)` | 包下的所有方法 |
| `execution(* com.example.*.*(..))` | 特定方法名 |
| `@within(RequirePermission)` | 类上有该注解 |

---

## 下一步

确认理解了AOP权限校验后，告诉我，我继续给你**步骤9：审计日志切面 + 异步写入**的代码。

审计日志是另一个AOP的经典应用场景，会用到 `@AfterReturning` 和 `@AfterThrowing`。