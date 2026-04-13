好的，我来**逐行讲解**步骤6的用户管理代码，让你理解每一部分的**设计意图**和**为什么这么写**。

---

## 一、DTO/VO 分层讲解

### 1.1 为什么分这么多类？

```
前端请求 → UserPageRequest (接收分页参数)
         → UserRequest (接收新增参数)
         → UserUpdateRequest (接收修改参数)
         
数据库 → User (实体)
         
返回前端 → UserResponse (返回用户信息)
        → PageResult (包装分页结果)
```

**核心原则：每个类只做一件事**

| 类名 | 用途 | 为什么不能复用 |
| :--- | :--- | :--- |
| `UserPageRequest` | 分页查询参数 | 包含`pageNum/pageSize`，新增不需要 |
| `UserRequest` | 新增用户参数 | 密码必填，修改时密码可选 |
| `UserUpdateRequest` | 修改用户参数 | ID必填，其他可选 |
| `UserResponse` | 返回用户信息 | 不包含密码，额外包含`roles`字段 |
| `User` (Entity) | 数据库映射 | 包含密码、审计字段，不能直接暴露给前端 |

**企业规范：Entity 永远不直接返回给前端**

```java
// ❌ 错误做法
@GetMapping("/{id}")
public User getById(@PathVariable Long id) {
    return userService.getById(id);  // 直接把Entity返回，密码会泄露
}

// ✅ 正确做法
@GetMapping("/{id}")
public Result<UserResponse> getById(@PathVariable Long id) {
    UserResponse response = userService.getById(id);
    return Result.success(response);  // 返回Response对象，不含密码
}
```

### 1.2 UserPageRequest 详解

```java
@Data
public class UserPageRequest {
    
    @Min(value = 1, message = "页码最小为1")
    private Integer pageNum = 1;   // 默认第1页
    
    @Min(value = 1, message = "每页条数最小为1")
    private Integer pageSize = 10; // 默认每页10条
    
    private String username;   // 模糊查询，可选
    private Integer status;     // 状态筛选，可选
}
```

**为什么要有默认值？**
- 前端不传`pageNum`时，默认查第1页
- 避免空指针异常

**为什么要加`@Min`校验？**
- 防止恶意请求传`pageNum=-1`或`pageSize=10000`导致数据库压力过大

### 1.3 UserRequest 详解

```java
@Data
public class UserRequest {
    
    @NotBlank(message = "用户名不能为空")
    @Size(min = 3, max = 20, message = "用户名长度3-20位")
    @Pattern(regexp = "^[a-zA-Z0-9_]+$", message = "用户名只能包含字母、数字、下划线")
    private String username;
    
    @NotBlank(message = "密码不能为空")
    @Size(min = 6, max = 20, message = "密码长度6-20位")
    private String password;
    
    private String nickname;        // 可选
    private Integer status = 1;     // 默认正常
}
```

**校验注解说明**：

| 注解 | 作用 | 示例 |
| :--- | :--- | :--- |
| `@NotBlank` | 不能为null，不能为空字符串 | `" "` 会报错 |
| `@Size` | 长度限制 | 用户名3-20位 |
| `@Pattern` | 正则表达式校验 | 只能包含字母数字下划线 |

**为什么用户名要加正则？**
- 防止用户注册`admin' OR '1'='1`这样的用户名
- 防止特殊字符导致日志解析问题

---

## 二、Service 层讲解

### 2.1 分页查询逻辑

```java
public PageResult<UserResponse> pageList(UserPageRequest request) {
    // 1. 构建分页对象
    Page<User> page = new Page<>(request.getPageNum(), request.getPageSize());
    
    // 2. 构建查询条件
    LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
    if (StringUtils.hasText(request.getUsername())) {
        wrapper.like(User::getUsername, request.getUsername());
    }
    if (request.getStatus() != null) {
        wrapper.eq(User::getStatus, request.getStatus());
    }
    wrapper.orderByDesc(User::getCreateTime);
    
    // 3. 执行查询
    IPage<User> userPage = userMapper.selectPage(page, wrapper);
    
    // 4. 转换为Response
    List<UserResponse> list = userPage.getRecords().stream()
            .map(this::convertToResponse)
            .collect(Collectors.toList());
    
    return new PageResult<>(userPage.getTotal(), list);
}
```

**流程图**：
```
前端请求: /user/page?pageNum=1&pageSize=10&username=admin
                    ↓
构建 Page(1, 10) + wrapper(username like '%admin%')
                    ↓
MyBatis-Plus 自动生成 SQL:
SELECT * FROM user 
WHERE username LIKE '%admin%' AND deleted=0 
ORDER BY create_time DESC 
LIMIT 0, 10
                    ↓
同时执行 COUNT SQL:
SELECT COUNT(*) FROM user WHERE username LIKE '%admin%' AND deleted=0
                    ↓
返回 PageResult(total=5, list=[...])
```

### 2.2 新增用户逻辑

```java
@Transactional
public void add(UserRequest request) {
    // 1. 检查用户名是否已存在
    LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
    wrapper.eq(User::getUsername, request.getUsername());
    if (userMapper.selectCount(wrapper) > 0) {
        throw new BusinessException(ErrorCode.USERNAME_EXISTS);
    }
    
    // 2. 创建用户
    User user = new User();
    user.setUsername(request.getUsername());
    user.setPassword(passwordEncoder.encode(request.getPassword()));  // 密码加密
    user.setNickname(request.getNickname());
    user.setStatus(request.getStatus());
    
    userMapper.insert(user);
}
```

**为什么要加 `@Transactional`？**
- 保证多个数据库操作在同一事务中
- 如果中间抛异常，所有操作回滚

**为什么要先检查用户名是否存在？**
- 数据库虽然有唯一索引，但提前检查可以给出更友好的错误提示
- 避免触发数据库异常（`DuplicateKeyException`）

**密码加密的时机**：
```
用户输入 "123456"
      ↓
passwordEncoder.encode("123456")
      ↓
生成 "$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EfMlqYy7JkI3q5q5q5q5q"
      ↓
存入数据库
```

**注意**：存的是加密后的字符串，原始密码永远不存。

### 2.3 更新用户逻辑

```java
@Transactional
public void update(UserUpdateRequest request) {
    // 1. 检查用户是否存在
    User user = userMapper.selectById(request.getId());
    if (user == null) {
        throw new BusinessException(ErrorCode.USER_NOT_FOUND);
    }
    
    // 2. 只更新传入的字段
    if (StringUtils.hasText(request.getNickname())) {
        user.setNickname(request.getNickname());
    }
    if (request.getStatus() != null) {
        user.setStatus(request.getStatus());
    }
    if (StringUtils.hasText(request.getPassword())) {
        user.setPassword(passwordEncoder.encode(request.getPassword()));
    }
    
    userMapper.updateById(user);
}
```

**为什么先查询再更新？**
- MyBatis-Plus的`updateById`只更新非null字段
- 但我们需要判断：如果密码传了空字符串，是不更新；如果传了值，才更新

**为什么不直接 `updateById` 传入新对象？**
```java
// ❌ 这样会出问题
User user = new User();
user.setId(request.getId());
user.setNickname(request.getNickname());  // 如果nickname为null，会把数据库的nickname设为null
userMapper.updateById(user);

// ✅ 正确做法：先查出来，只更新需要改的字段
```

### 2.4 删除用户逻辑

```java
@Transactional
public void delete(Long id) {
    int result = userMapper.deleteById(id);
    if (result == 0) {
        throw new BusinessException(ErrorCode.USER_NOT_FOUND);
    }
}
```

**实际执行的SQL**（因为有`@TableLogic`）：
```sql
-- 不是 DELETE FROM user WHERE id=?
-- 而是：
UPDATE user SET deleted=1 WHERE id=?
```

**为什么用逻辑删除？**
- 数据可恢复（误删时改回deleted=0即可）
- 审计要求（不能彻底消失）
- 历史数据追溯

### 2.5 实体转Response

```java
private UserResponse convertToResponse(User user) {
    UserResponse response = new UserResponse();
    response.setId(user.getId());
    response.setUsername(user.getUsername());
    response.setNickname(user.getNickname());
    response.setStatus(user.getStatus());
    response.setCreateTime(user.getCreateTime());
    
    // 额外查询用户的角色
    List<String> roles = roleMapper.selectRoleCodesByUserId(user.getId());
    response.setRoles(roles);
    
    return response;
}
```

**为什么需要这个转换方法？**
- Entity有密码字段，Response没有（安全）
- Response需要额外的`roles`字段（Entity里没有）
- 分离后，改数据库不影响前端接口

---

## 三、Controller 层讲解

### 3.1 权限控制

```java
@GetMapping("/page")
@PreAuthorize("hasAuthority('user:list')")
public Result<PageResult<UserResponse>> pageList(@Valid UserPageRequest request) {
    // ...
}
```

**`@PreAuthorize` 工作原理**：
```
请求进入 → Spring Security拦截
              ↓
检查当前用户的权限列表是否包含 'user:list'
              ↓
有 → 执行方法
无 → 抛出 AccessDeniedException → 全局异常处理器返回403
```

**权限数据从哪里来？**
后续步骤会实现：
1. 用户登录时，从数据库查询用户的所有权限码
2. 存入Redis（key: `auth:user:1:permissions`）
3. JWT过滤器从Redis读取并存入SecurityContext

### 3.2 参数校验

```java
public Result<PageResult<UserResponse>> pageList(@Valid UserPageRequest request)
```

`@Valid` 会触发DTO中的校验注解：
- `@Min(1)` 校验pageNum ≥ 1
- 校验失败时，全局异常处理器捕获并返回友好提示

### 3.3 RESTful 接口设计

| HTTP方法 | URL | 含义 | 幂等性 |
| :--- | :--- | :--- | :--- |
| GET | `/user/page` | 分页查询 | 是 |
| GET | `/user/{id}` | 查询详情 | 是 |
| POST | `/user` | 新增 | 否（多次创建会重复） |
| PUT | `/user` | 修改 | 是 |
| DELETE | `/user/{id}` | 删除 | 是 |

**幂等性**：多次调用结果相同。

---

## 四、MyBatis-Plus 配置讲解

### 4.1 分页插件

```java
@Bean
public MybatisPlusInterceptor mybatisPlusInterceptor() {
    MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();
    interceptor.addInnerInterceptor(new PaginationInnerInterceptor(DbType.MYSQL));
    return interceptor;
}
```

**作用**：自动拦截`selectPage`方法，改写SQL添加`LIMIT`。

### 4.2 乐观锁插件

```java
interceptor.addInnerInterceptor(new OptimisticLockerInnerInterceptor());
```

配合Entity中的`@Version`字段：
```sql
-- 更新时自动加上版本号条件
UPDATE user SET nickname='新昵称', version=version+1 
WHERE id=1 AND version=0
```

**作用**：防止并发更新导致数据覆盖。

---

## 五、测试流程讲解

### 5.1 正常流程

```
1. 登录admin → 获取token
2. 请求 GET /user/page（携带token）
3. JWT过滤器验证token → 存入SecurityContext
4. @PreAuthorize检查权限 → 通过
5. 执行方法 → 返回用户列表
```

### 5.2 无权限流程

```
1. 登录普通用户 → 获取token
2. 请求 DELETE /user/1（携带token）
3. JWT过滤器验证token → 存入SecurityContext（只有user:list权限）
4. @PreAuthorize检查 hasAuthority('user:delete') → 失败
5. 抛出AccessDeniedException → 返回403
```

### 5.3 Token过期流程

```
1. 登录获取token（30分钟有效）
2. 31分钟后请求接口
3. JWT过滤器验证token → 过期
4. SecurityContext没有认证信息
5. @PreAuthorize拦截 → 返回401
```

---

## 六、常见问题解答

### Q1：为什么UserService接口和实现类要分开？

```java
// 接口
public interface UserService { ... }

// 实现类
@Service
public class UserServiceImpl implements UserService { ... }
```

**原因**：
- 方便AOP代理（Spring默认使用JDK动态代理）
- 可以轻松替换实现（比如切换成UserServiceMock用于测试）
- 符合依赖倒置原则

### Q2：为什么用LambdaQueryWrapper而不是字符串？

```java
// Lambda方式（推荐）
wrapper.like(User::getUsername, username);

// 字符串方式（不推荐）
wrapper.like("username", username);
```

**Lambda优势**：字段名写错会编译报错，字符串写错要运行时才发现。

### Q3：事务注解加在Controller还是Service？

```java
// ✅ 正确：加在Service层
@Service
@Transactional
public class UserServiceImpl { ... }

// ❌ 错误：加在Controller层
@RestController
@Transactional  // Controller不应该处理事务
public class UserController { ... }
```

**原因**：事务应该包含业务操作，Controller只负责接收请求和返回响应。

---

## 下一步

如果你理解了以上内容，告诉我，我继续给你**步骤7：角色管理 + 权限分配**的代码。

如果有任何不理解的地方，指出具体哪一段，我再深入讲解。