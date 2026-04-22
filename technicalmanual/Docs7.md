好的，我来**逐行讲解**步骤11的单元测试代码，让你理解**为什么要写测试**、**怎么写出好的测试**。

---

## 一、为什么需要单元测试？

### 1.1 没有测试的后果

```java
// 你修改了 UserService 的代码
public void add(UserRequest request) {
    // 原来：userMapper.insert(user);
    // 你改成了：userMapper.insertSelective(user);
    
    // 问题：insertSelective 可能不存在，但你没有测试，直接部署了
    // 结果：线上报错 NoSuchMethodError
}
```

### 1.2 有测试的好处

| 场景 | 没有测试 | 有测试 |
| :--- | :--- | :--- |
| 修改代码后 | 手动点一遍功能，可能漏测 | 运行测试，5秒知道哪里坏了 |
| 重构代码 | 心惊胆战，怕改坏 | 测试通过就放心 |
| 新人接手 | 不知道代码怎么用 | 看测试就知道用法 |
| 面试 | 没法证明代码质量 | 展示测试覆盖率80%+ |

**企业要求**：核心代码测试覆盖率 > 80%，否则不能合并到主分支。

---

## 二、测试依赖讲解

```xml
<!-- Spring Boot Test：整合了JUnit、Mockito等 -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-test</artifactId>
    <scope>test</scope>
</dependency>

<!-- H2 内存数据库 -->
<dependency>
    <groupId>com.h2database</groupId>
    <artifactId>h2</artifactId>
    <scope>test</scope>
</dependency>
```

**H2 是什么？**
- 内存数据库，测试时创建，测试完自动销毁
- 不需要安装MySQL，测试更快、更干净
- 配置 `jdbc:h2:mem:testdb` 即可

**为什么用 H2 而不是真实 MySQL？**

| 方案 | 优点 | 缺点 |
| :--- | :--- | :--- |
| 真实 MySQL | 环境真实 | 慢，需要安装，测试间互相影响 |
| H2 内存库 | 快，无需安装，测试隔离 | 语法与MySQL略有差异 |

---

## 三、测试配置文件讲解

```yaml
spring:
  datasource:
    url: jdbc:h2:mem:testdb;MODE=MySQL  # 使用H2，兼容MySQL模式
    username: sa
    password:
  h2:
    console:
      enabled: true  # 可以浏览器访问 http://localhost:8080/h2-console
```

**`MODE=MySQL` 的作用**：
- H2 默认语法和 MySQL 不一样
- 设置这个后，H2 会尽量兼容 MySQL 的SQL语法

---

## 四、测试基类讲解

```java
@SpringBootTest        // 启动完整的Spring容器
@ActiveProfiles("test") // 使用 test 配置文件
@ExtendWith(MockitoExtension.class)  // 集成 Mockito
public abstract class BaseTest {
}
```

**`@SpringBootTest` 的作用**：
- 启动整个 Spring 容器（包括所有 Bean）
- 适合集成测试

**`@ExtendWith(MockitoExtension.class)` 的作用**：
- 让 `@Mock`、`@InjectMocks` 等注解生效

---

## 五、JwtUtilsTest 讲解

```java
@DisplayName("JWT工具类测试")  // 测试类的显示名称
class JwtUtilsTest extends BaseTest {
    
    @Autowired
    private JwtUtils jwtUtils;  // 自动注入真实对象
    
    @BeforeEach  // 每个测试方法执行前运行
    void setUp() {
        testUserId = 1L;
        testUsername = "admin";
        token = jwtUtils.generateToken(testUserId, testUsername);
    }
    
    @Test
    @DisplayName("生成Token - 成功")
    void testGenerateToken() {
        assertNotNull(token);  // 验证 token 不为 null
        assertTrue(token.length() > 0);  // 验证 token 长度 > 0
    }
}
```

**`@BeforeEach` vs `@BeforeAll`**：

| 注解 | 执行时机 | 适用场景 |
| :--- | :--- | :--- |
| `@BeforeEach` | 每个测试方法前 | 准备测试数据（每个测试需要独立数据） |
| `@BeforeAll` | 所有测试方法前执行一次 | 初始化耗时资源（如数据库连接） |

**`@DisplayName` 的作用**：
- 让测试报告更易读
- 运行结果显示：`JWT工具类测试 > 生成Token - 成功` 而不是 `testGenerateToken`

---

## 六、UserServiceTest 讲解（核心）

### 6.1 Mock 的使用

```java
@MockBean  // 模拟 UserMapper（不连接真实数据库）
private UserMapper userMapper;

@Autowired  // 注入真实的 UserService
private UserService userService;
```

**`@MockBean` vs `@Mock`**：

| 注解 | 适用环境 | 说明 |
| :--- | :--- | :--- |
| `@MockBean` | Spring Boot 测试 | Mock 的对象会被放入 Spring 容器 |
| `@Mock` | 纯 JUnit 测试 | 不启动 Spring 容器 |

**为什么要 Mock？**

```
真实调用链：
UserService → UserMapper → MySQL
              ↑
         这里被 Mock 替换

Mock 调用链：
UserService → UserMapper (模拟对象，返回预设数据)
              ↑
         不连数据库，速度快
```

### 6.2 测试成功场景

```java
@Test
@DisplayName("根据ID查询用户 - 成功")
void testGetByIdSuccess() {
    // Arrange：准备测试数据
    when(userMapper.selectById(1L)).thenReturn(testUser);  // 模拟返回
    when(roleMapper.selectRoleCodesByUserId(1L)).thenReturn(List.of("ADMIN"));
    
    // Act：执行被测试的方法
    UserResponse response = userService.getById(1L);
    
    // Assert：验证结果
    assertNotNull(response);
    assertEquals(1L, response.getId());
    assertEquals("admin", response.getUsername());
    
    // Verify：验证 Mock 的方法被正确调用
    verify(userMapper, times(1)).selectById(1L);
}
```

**`when().thenReturn()` 的作用**：
```java
// 当调用 userMapper.selectById(1L) 时，返回 testUser
when(userMapper.selectById(1L)).thenReturn(testUser);
```

**`verify()` 的作用**：
```java
// 验证 userMapper.selectById(1L) 被调用了 1 次
verify(userMapper, times(1)).selectById(1L);
```

**`times()` 的参数**：

| 参数 | 含义 |
| :--- | :--- |
| `times(1)` | 恰好调用1次 |
| `never()` | 从未调用 |
| `atLeast(1)` | 至少1次 |
| `atMost(3)` | 最多3次 |

### 6.3 测试异常场景

```java
@Test
@DisplayName("根据ID查询用户 - 用户不存在抛异常")
void testGetByIdNotFound() {
    when(userMapper.selectById(999L)).thenReturn(null);
    
    // 断言：执行 userService.getById(999L) 会抛出 BusinessException
    BusinessException exception = assertThrows(BusinessException.class, () -> {
        userService.getById(999L);
    });
    
    // 验证异常的错误码
    assertEquals(ErrorCode.USER_NOT_FOUND.getCode(), exception.getCode());
}
```

**`assertThrows` 的作用**：
- 执行括号内的代码
- 如果抛出指定类型的异常，测试通过
- 如果没有抛出异常，测试失败

### 6.4 验证方法未被调用

```java
@Test
@DisplayName("新增用户 - 用户名已存在抛异常")
void testAddDuplicateUsername() {
    // ...
    BusinessException exception = assertThrows(BusinessException.class, () -> {
        userService.add(request);
    });
    
    // 验证：userMapper.insert() 从未被调用
    verify(userMapper, never()).insert(any());
}
```

**`never()` 的作用**：确保某些代码在异常情况下不会执行。

---

## 七、PermissionServiceTest 讲解

### 7.1 Mock Redis 操作

```java
@MockBean
private RedisTemplate<String, String> redisTemplate;

@MockBean
private SetOperations<String, String> setOperations;  // Redis Set 操作

@BeforeEach
void setUp() {
    when(redisTemplate.opsForSet()).thenReturn(setOperations);
}
```

**为什么需要 Mock `SetOperations`？**
- `redisTemplate.opsForSet()` 返回一个对象
- 我们需要模拟这个对象的行为

### 7.2 测试缓存命中

```java
@Test
void testGetUserPermissionsFromCache() {
    // 模拟：缓存中有数据
    when(setOperations.members(cacheKey)).thenReturn(cachedPermissions);
    
    Set<String> permissions = permissionService.getUserPermissions(userId);
    
    // 验证：没有查数据库
    verify(userRoleMapper, never()).selectRoleIdsByUserId(any());
}
```

### 7.3 测试缓存未命中

```java
@Test
void testGetUserPermissionsFromDB() {
    // 第一次：缓存为空
    when(setOperations.members(cacheKey)).thenReturn(Set.of());
    
    // 模拟数据库返回
    when(userRoleMapper.selectRoleIdsByUserId(userId)).thenReturn(roleIds);
    when(permissionMapper.selectPermissionCodesByRoleId(1L)).thenReturn(List.of("user:list"));
    
    // 执行
    Set<String> permissions = permissionService.getUserPermissions(userId);
    
    // 验证：缓存被写入
    verify(setOperations, times(3)).add(eq(cacheKey), anyString());
}
```

**`eq()` 的作用**：匹配参数，`anyString()` 匹配任意字符串。

---

## 八、测试数据文件讲解

### 8.1 schema.sql

```sql
CREATE TABLE IF NOT EXISTS `user` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `username` VARCHAR(50) NOT NULL,
    ...
);
```

**作用**：在 H2 中创建表结构。

### 8.2 data.sql

```sql
INSERT INTO `user` (`id`, `username`, ...) VALUES
(1, 'admin', ...);
```

**作用**：插入初始测试数据。

---

## 九、运行测试

### 9.1 IDEA 中运行

```
右键 src/test/java → Run 'All Tests'
```

### 9.2 查看测试覆盖率

```
右键 src/test/java → Run with Coverage
```

**覆盖率颜色含义**：
- 🟢 绿色：已覆盖
- 🟡 黄色：部分覆盖（如只执行了if的一个分支）
- 🔴 红色：未覆盖

### 9.3 覆盖率目标

| 层级 | 目标 | 原因 |
| :--- | :--- | :--- |
| Utils | > 90% | 工具类容易测试 |
| Service | > 80% | 核心业务逻辑 |
| Controller | > 60% | 涉及HTTP，测试较麻烦 |

---

## 十、常见问题解答

### Q1：测试运行失败，提示找不到Bean

**原因**：`@SpringBootTest` 会启动整个容器，但测试配置文件中可能缺少某些配置。

**解决**：使用 `@MockBean` 模拟缺失的Bean。

### Q2：测试之间互相影响

**原因**：使用了静态变量或共享数据。

**解决**：使用 `@BeforeEach` 在每个测试前重新初始化数据。

### Q3：什么时候用 `@MockBean`，什么时候用 `@Autowired`？

| 被测对象 | 使用 | 说明 |
| :--- | :--- | :--- |
| 被测试的Service | `@Autowired` | 需要真实对象 |
| Service依赖的Mapper | `@MockBean` | 模拟数据库操作 |
| Service依赖的其他Service | `@MockBean` | 隔离测试 |

---

## 十一、好的测试应该满足的条件

| 条件 | 说明 | 反例 |
| :--- | :--- | :--- |
| **独立性** | 测试之间不依赖 | 测试A先运行，测试B才能运行 ❌ |
| **可重复性** | 多次运行结果相同 | 依赖当前时间 ❌ |
| **快速** | 毫秒级完成 | 每次测试等10秒 ❌ |
| **单一职责** | 一个测试只测一件事 | 一个测试里断言10个东西 ❌ |
| **可读性** | 看测试就知道代码怎么用 | 变量名是a、b、c ❌ |

---

## 十二、测试命名规范

```java
// 格式：test{方法名}_{场景}_{预期结果}
testGetById_Success_ReturnUser()
testGetById_UserNotFound_ThrowException()
testAdd_DuplicateUsername_ThrowException()

// 或使用 @DisplayName
@DisplayName("根据ID查询用户 - 成功")
void testGetByIdSuccess() { ... }
```

---

## 下一步

理解了单元测试后，告诉我，我继续给你**步骤12：Docker部署**的代码。这是最后一步，做完后整个项目就完整了。