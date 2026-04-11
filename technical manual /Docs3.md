好的，我来**逐行讲解**步骤7的角色管理和权限分配代码，这是RBAC权限模型的核心。

---

## 一、RBAC 核心概念讲解

### 1.1 什么是RBAC？

**RBAC** = Role-Based Access Control（基于角色的访问控制）

```
用户 → 角色 → 权限

举例：
张三 → 管理员 → [创建用户, 删除用户, 查看日志]
李四 → 普通用户 → [查看用户列表]
王五 → 访客 → [无权限]
```

**为什么用角色而不是直接给用户分配权限？**

| 方案 | 优点 | 缺点 |
| :--- | :--- | :--- |
| 用户直接绑权限 | 灵活 | 1000个用户要配1000次，改权限要改1000人 |
| 用户→角色→权限 | 批量管理 | 多一层关系 |

企业里都是RBAC，因为权限变更是常态。比如“所有市场部的人都要加一个权限”，只需要改市场部角色，不用改100个人。

### 1.2 三张核心表的关系

```
┌──────────┐     ┌──────────────┐     ┌──────────┐
│   user   │────<│   user_role  │>────│   role   │
│  用户表   │     │  用户角色关联表 │     │  角色表   │
└──────────┘     └──────────────┘     └──────────┘
                                              │
                                              │
                                        ┌─────┴─────┐
                                        │           │
                                  ┌──────────────┐ ┌──────────────┐
                                  │role_permission│ │ permission   │
                                  │ 角色权限关联表  │ │   权限表     │
                                  └──────────────┘ └──────────────┘
```

**实际数据示例**：

| user表 | role表 | permission表 |
| :--- | :--- | :--- |
| id=1, name=admin | id=1, code=ADMIN | id=1, code=user:list |
| id=2, name=张三 | id=2, code=USER | id=2, code=user:add |

| user_role表 | role_permission表 |
| :--- | :--- |
| user_id=1, role_id=1 | role_id=1, permission_id=1 |
| user_id=2, role_id=2 | role_id=1, permission_id=2 |

**查询张三的权限**：
```
张三(user_id=2) → user_role查到role_id=2 → role_permission查到permission_id=1 → permission查到code=user:list
```

---

## 二、实体类讲解

### 2.1 Permission.java

```java
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("permission")
public class Permission extends BaseEntity {
    private String permissionCode;   // 权限码，如 "user:add"
    private String permissionName;   // 权限名称，如 "新增用户"
    private Long parentId;           // 父权限ID，0表示顶级
    private Integer sort;            // 排序，数字越小越靠前
}
```

**parentId的作用**：实现权限树结构

```
parentId=0 (顶级)
├── 用户管理 (parentId=1)
│   ├── 查询用户 (parentId=2)
│   ├── 新增用户 (parentId=3)
│   └── 删除用户 (parentId=4)
├── 角色管理 (parentId=5)
│   ├── 查询角色 (parentId=6)
│   └── 新增角色 (parentId=7)
```

前端可以渲染成树形菜单，比如左侧导航栏。

### 2.2 UserRole.java 和 RolePermission.java

```java
// 用户-角色关联表实体
@Data
@TableName("user_role")
public class UserRole {
    private Long id;
    private Long userId;   // 用户ID
    private Long roleId;   // 角色ID
}

// 角色-权限关联表实体
@Data
@TableName("role_permission")
public class RolePermission {
    private Long id;
    private Long roleId;       // 角色ID
    private Long permissionId; // 权限ID
}
```

**为什么需要这两张关联表？**

因为一个用户可以拥有多个角色，一个角色也可以属于多个用户。这是**多对多关系**，需要中间表。

```
user表(1) ─────(N) user_role表(N) ─────(1) role表
```

---

## 三、DTO讲解

### 3.1 AssignPermissionsRequest.java

```java
@Data
public class AssignPermissionsRequest {
    @NotNull(message = "角色ID不能为空")
    private Long roleId;
    private List<Long> permissionIds;  // 要分配的权限ID列表
}
```

**为什么用List？**
一次可以分配多个权限，减少HTTP请求次数。

**前端调用示例**：
```json
{
    "roleId": 1,
    "permissionIds": [1, 2, 3, 4, 5]
}
```

### 3.2 AssignRolesRequest.java

```java
@Data
public class AssignRolesRequest {
    @NotNull(message = "用户ID不能为空")
    private Long userId;
    private List<Long> roleIds;  // 要分配的角色ID列表
}
```

**前端调用示例**：
```json
{
    "userId": 2,
    "roleIds": [1, 2]  // 给用户同时分配管理员和普通用户角色
}
```

---

## 四、Mapper层讲解

### 4.1 PermissionMapper.java

```java
@Select("SELECT p.permission_code FROM permission p " +
        "LEFT JOIN role_permission rp ON p.id = rp.permission_id " +
        "WHERE rp.role_id = #{roleId} AND p.deleted = 0")
List<String> selectPermissionCodesByRoleId(@Param("roleId") Long roleId);
```

**SQL解析**：
```sql
SELECT p.permission_code 
FROM permission p 
LEFT JOIN role_permission rp ON p.id = rp.permission_id 
WHERE rp.role_id = 1 AND p.deleted = 0
```

执行结果示例：
```
permission_code
---------------
user:list
user:add
user:delete
role:list
```

**为什么用LEFT JOIN而不是INNER JOIN？**
- 实际上这里用INNER JOIN效果一样，因为WHERE条件过滤了role_id
- 但习惯用LEFT JOIN表示"以左表为主"

### 4.2 UserRoleMapper.java

```java
@Delete("DELETE FROM user_role WHERE user_id = #{userId}")
int deleteByUserId(@Param("userId") Long userId);
```

**这是"先删后增"模式的关键**：
```java
// 分配角色时：
deleteByUserId(userId);  // 1. 先删除所有旧角色
for (roleId : newRoleIds) {
    insert(...);          // 2. 再插入新角色
}
```

**为什么不用UPDATE？**
- 如果用户原有角色[1,2]，新角色[2,3]
- 需要比较差异：删除1，新增3
- "先删后增"更简单，不需要比较逻辑
- 缺点是会触发两次操作，但角色分配不是高频操作，可以接受

### 4.3 RolePermissionMapper.java

```java
@Select("SELECT permission_id FROM role_permission WHERE role_id = #{roleId}")
List<Long> selectPermissionIdsByRoleId(@Param("roleId") Long roleId);
```

**作用**：查询角色已有的权限ID列表，用于前端回显（编辑角色时，勾选已有的权限）。

---

## 五、Service层讲解

### 5.1 角色列表查询

```java
public List<RoleResponse> list() {
    LambdaQueryWrapper<Role> wrapper = new LambdaQueryWrapper<>();
    wrapper.orderByAsc(Role::getRoleCode);  // 按角色代码升序
    
    List<Role> roles = roleMapper.selectList(wrapper);
    return roles.stream()
            .map(this::convertToResponse)  // 每个Role转成RoleResponse
            .collect(Collectors.toList());
}
```

**为什么返回List而不是Page？**
- 角色数量通常很少（几十个以内）
- 前端需要全部角色来做下拉框
- 不需要分页

### 5.2 权限分配（核心逻辑）

```java
@Transactional
public void assignPermissions(AssignPermissionsRequest request) {
    Long roleId = request.getRoleId();
    
    // 1. 检查角色是否存在
    Role role = roleMapper.selectById(roleId);
    if (role == null) {
        throw new BusinessException(ErrorCode.ROLE_NOT_FOUND);
    }
    
    // 2. 删除原有权限关联（清空）
    rolePermissionMapper.deleteByRoleId(roleId);
    
    // 3. 添加新的权限关联
    List<Long> permissionIds = request.getPermissionIds();
    if (permissionIds != null && !permissionIds.isEmpty()) {
        for (Long permissionId : permissionIds) {
            RolePermission rp = new RolePermission();
            rp.setRoleId(roleId);
            rp.setPermissionId(permissionId);
            rolePermissionMapper.insert(rp);
        }
    }
}
```

**事务流程图**：
```
开始事务
    ↓
检查角色是否存在（不存在则抛异常，事务回滚）
    ↓
DELETE FROM role_permission WHERE role_id = 1
    ↓
INSERT INTO role_permission (role_id, permission_id) VALUES (1, 1)
INSERT INTO role_permission (role_id, permission_id) VALUES (1, 2)
INSERT INTO role_permission (role_id, permission_id) VALUES (1, 3)
    ↓
提交事务（如果任何一步失败，全部回滚）
```

**为什么用`@Transactional`？**
- 如果删除成功但插入失败，数据会变成空（危险）
- 事务保证：要么全部成功，要么全部回滚到分配前的状态

### 5.3 权限树构建（递归算法）

```java
public List<PermissionTreeVO> getPermissionTree() {
    // 1. 查询所有权限
    List<Permission> permissions = permissionMapper.selectAllPermissions();
    // 2. 递归构建树
    return buildTree(permissions, 0L);
}

private List<PermissionTreeVO> buildTree(List<Permission> permissions, Long parentId) {
    return permissions.stream()
            .filter(p -> p.getParentId().equals(parentId))  // 筛选当前层级的节点
            .map(p -> {
                PermissionTreeVO node = new PermissionTreeVO();
                node.setId(p.getId());
                node.setLabel(p.getPermissionName());
                node.setCode(p.getPermissionCode());
                // 递归：子节点的parentId = 当前节点id
                node.setChildren(buildTree(permissions, p.getId()));
                return node;
            })
            .collect(Collectors.toList());
}
```

**递归过程示例**（数据）：
```
权限列表：
id=1, name=用户管理, parentId=0
id=2, name=查询用户, parentId=1
id=3, name=新增用户, parentId=1
id=4, name=角色管理, parentId=0
id=5, name=查询角色, parentId=4

调用 buildTree(所有权限, parentId=0)：
  筛选出 parentId=0 的节点：[用户管理(id=1), 角色管理(id=4)]
  
  处理 id=1（用户管理）：
    调用 buildTree(所有权限, parentId=1)：
      筛选出 parentId=1 的节点：[查询用户(id=2), 新增用户(id=3)]
      这两个没有子节点（children=[]）
    节点1的children = [节点2, 节点3]
  
  处理 id=4（角色管理）：
    调用 buildTree(所有权限, parentId=4)：
      筛选出 parentId=4 的节点：[查询角色(id=5)]
    节点4的children = [节点5]

返回树：
[
  {id:1, label:"用户管理", children:[
    {id:2, label:"查询用户", children:[]},
    {id:3, label:"新增用户", children:[]}
  ]},
  {id:4, label:"角色管理", children:[
    {id:5, label:"查询角色", children:[]}
  ]}
]
```

### 5.4 Role转RoleResponse

```java
private RoleResponse convertToResponse(Role role) {
    RoleResponse response = new RoleResponse();
    response.setId(role.getId());
    response.setRoleCode(role.getRoleCode());
    response.setRoleName(role.getRoleName());
    response.setCreateTime(role.getCreateTime());
    
    // 额外查询：这个角色有哪些权限
    List<String> permissions = permissionMapper.selectPermissionCodesByRoleId(role.getId());
    response.setPermissions(permissions);
    
    return response;
}
```

**为什么要在转换时查询权限？**
- 前端编辑角色时，需要展示这个角色已有的权限（打勾状态）
- 一次请求把角色信息和权限信息都返回，减少HTTP请求

---

## 六、Controller层讲解

### 6.1 RESTful接口设计

| 方法 | 路径 | 权限 | 说明 |
| :--- | :--- | :--- | :--- |
| GET | `/role/list` | role:list | 查询角色列表 |
| GET | `/role/{id}` | role:list | 查询角色详情 |
| POST | `/role` | role:add | 新增角色 |
| PUT | `/role` | role:edit | 修改角色 |
| DELETE | `/role/{id}` | role:delete | 删除角色 |
| POST | `/role/permissions` | role:edit | 分配权限 |
| GET | `/role/permission-tree` | role:list | 获取权限树 |

### 6.2 分配权限接口

```java
@PostMapping("/permissions")
@PreAuthorize("hasAuthority('role:edit')")
public Result<Void> assignPermissions(@Valid @RequestBody AssignPermissionsRequest request) {
    roleService.assignPermissions(request);
    return Result.success();
}
```

**为什么分配权限需要`role:edit`权限？**
- 分配权限是修改角色的一种方式
- 和修改角色名称、描述属于同一级别
- 不需要单独设计`role:assign-permission`权限

### 6.3 用户角色分配接口

```java
@PostMapping("/assign")
@PreAuthorize("hasAuthority('user:edit')")
public Result<Void> assignRoles(@Valid @RequestBody AssignRolesRequest request) {
    userRoleService.assignRoles(request);
    return Result.success();
}
```

**为什么分配角色是`user:edit`而不是`role:edit`？**
- 逻辑上：你在"编辑用户"，给用户加角色
- 不是直接编辑角色本身
- 所以用`user:edit`更合理

---

## 七、完整数据流讲解

### 7.1 给用户分配角色流程

```
前端操作：用户管理页面 → 点击"分配角色" → 勾选角色 → 提交
    ↓
POST /user-role/assign
Body: {"userId": 2, "roleIds": [1, 2]}
    ↓
UserRoleController.assignRoles()
    ↓
UserRoleService.assignRoles()
    ↓
1. 检查用户是否存在
2. DELETE FROM user_role WHERE user_id = 2
3. INSERT INTO user_role (user_id, role_id) VALUES (2, 1)
4. INSERT INTO user_role (user_id, role_id) VALUES (2, 2)
    ↓
返回成功
```

### 7.2 查询用户权限流程（登录时）

```
用户登录：POST /auth/login
    ↓
AuthService.login()
    ↓
1. 验证用户名密码
2. 查询用户ID
3. 查询用户角色：SELECT role_id FROM user_role WHERE user_id = 2 → [1, 2]
4. 查询角色权限：SELECT permission_id FROM role_permission WHERE role_id IN (1,2) → [1,2,3,4,5]
5. 查询权限码：SELECT permission_code FROM permission WHERE id IN (...) → ["user:list", "user:add", ...]
6. 存入Redis：key="auth:user:2:permissions", value="user:list,user:add,..."
7. 生成JWT返回
```

### 7.3 权限校验流程（请求时）

```
前端请求：GET /user/page，Header携带JWT
    ↓
JwtAuthenticationFilter：验证JWT，解析userId=2
    ↓
从Redis读取权限：GET auth:user:2:permissions → ["user:list", "user:add"]
    ↓
存入SecurityContext
    ↓
UserController.pageList() 上有 @PreAuthorize("hasAuthority('user:list')")
    ↓
Spring Security检查SecurityContext中是否有"user:list"
    ↓
有 → 执行方法
无 → 返回403
```

---

## 八、测试用例讲解

### 测试1：给普通用户分配查看权限

**目的**：让普通用户也能看到用户列表

```sql
-- 1. 给普通用户角色(GUEST)分配user:list权限
INSERT INTO role_permission (role_id, permission_id) 
SELECT 2, id FROM permission WHERE permission_code = 'user:list';

-- 2. 给普通用户分配GUEST角色
INSERT INTO user_role (user_id, role_id) VALUES (2, 2);
```

### 测试2：验证权限生效

```
1. 普通用户登录（username=user, password=123456）
2. 拿到token
3. 调用 GET /user/page（携带token）
4. 应该能返回用户列表
5. 调用 DELETE /user/1（删除用户）
6. 应该返回403
```

---

## 九、常见问题解答

### Q1：为什么要同时有role_code和role_name？

| 字段 | 用途 | 示例 |
| :--- | :--- | :--- |
| role_code | 代码中使用，不变 | `ADMIN`、`TEACHER` |
| role_name | 前端显示，可以改 | `管理员`、`教师` |

代码里判断角色用`role_code`，因为它是稳定的。角色名称可能被产品经理要求修改。

### Q2：权限码的命名规范？

```
格式：{资源}:{操作}

常见操作：
- list：列表查询
- add：新增
- edit：编辑
- delete：删除
- export：导出
- import：导入

示例：
user:list
user:add
order:export
log:delete
```

### Q3：为什么角色删除时要同时删除关联表数据？

```java
roleMapper.deleteById(id);           // 逻辑删除角色
rolePermissionMapper.deleteByRoleId(id);  // 删除角色-权限关联
```

如果只删角色不删关联：
- `role_permission`表会有孤儿数据（role_id指向不存在的角色）
- 查询时会报错或查到无效数据

---

## 下一步

如果你理解了以上内容，告诉我，我继续给你**步骤8：权限校验注解 + AOP实现**的代码。

这是实现`@PreAuthorize("hasAuthority('xxx')")`背后机制的关键步骤。