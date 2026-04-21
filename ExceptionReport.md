## 错误报告：Spring Boot 测试环境配置失败

---

### 一、错误概述

| 项目 | 内容 |
| :--- | :--- |
| **项目名称** | AccessControlSystem（企业级权限系统） |
| **错误类型** | Spring Boot 测试容器启动失败 |
| **影响范围** | 所有单元测试无法运行（17个测试，16个错误） |
| **严重程度** | 🔴 严重（测试完全不可用） |
| **发生时间** | 2026-04-21 19:51 |

---

### 二、错误现象

执行 `mvn test` 后，所有测试失败，Spring 容器无法启动。

**关键错误信息**：

```
1. Cannot read SQL script from class path resource [cleanup.sql] because it does not exist
2. Table "USER_ROLE" not found
3. Syntax error in SQL statement "DELETE FROM [*]user WHERE 1=1"
4. Column count does not match; SQL statement: MERGE INTO `user`...
```

---

### 三、错误链条分析

```
┌─────────────────────────────────────────────────────────────────┐
│ 1. 根本原因：测试环境 SQL 脚本配置错误                           │
│    - schema.sql 内容重复、有残留代码                             │
│    - data.sql 语法不兼容 H2 数据库                              │
│    - cleanup.sql 文件不存在                                     │
└─────────────────────────────────────────────────────────────────┘
                              ↓
┌─────────────────────────────────────────────────────────────────┐
│ 2. Spring Boot 启动测试容器时执行 SQL 初始化                      │
│    - 执行 schema.sql → 失败（语法错误）                          │
│    - 执行 data.sql → 失败（表不存在）                           │
│    - 执行 cleanup.sql → 文件不存在                               │
└─────────────────────────────────────────────────────────────────┘
                              ↓
┌─────────────────────────────────────────────────────────────────┐
│ 3. DataSourceScriptDatabaseInitializer 初始化失败               │
│    - 无法创建数据库表结构                                        │
└─────────────────────────────────────────────────────────────────┘
                              ↓
┌─────────────────────────────────────────────────────────────────┐
│ 4. sqlSessionTemplate 无法创建                                  │
│    - MyBatis-Plus 无法初始化                                    │
└─────────────────────────────────────────────────────────────────┘
                              ↓
┌─────────────────────────────────────────────────────────────────┐
│ 5. 所有 Mapper Bean 创建失败                                    │
│    - roleMapper, userMapper, userRoleMapper 等                  │
└─────────────────────────────────────────────────────────────────┘
                              ↓
┌─────────────────────────────────────────────────────────────────┐
│ 6. 依赖 Mapper 的 Bean 创建失败                                 │
│    - jwtAuthenticationFilter 需要 roleMapper                    │
│    - securityConfig 需要 jwtAuthenticationFilter                │
└─────────────────────────────────────────────────────────────────┘
                              ↓
┌─────────────────────────────────────────────────────────────────┐
│ 7. Spring 容器启动失败                                          │
│    - 所有测试无法运行                                            │
└─────────────────────────────────────────────────────────────────┘
```

---

### 四、具体错误明细

#### 4.1 错误1：cleanup.sql 文件不存在

```
java.io.FileNotFoundException: class path resource [cleanup.sql] cannot be opened because it does not exist
```

**原因**：测试类中使用了 `@Sql(scripts = "/cleanup.sql")` 注解，但 `src/test/resources/cleanup.sql` 文件不存在。

**涉及测试类**：`UserServiceTest`, `PermissionServiceTest`

#### 4.2 错误2：表不存在

```
Table "USER_ROLE" not found; SQL statement: DELETE FROM user_role WHERE 1=1
```

**原因**：`schema.sql` 执行失败，导致表结构未创建。`data.sql` 执行时找不到表。

#### 4.3 错误3：SQL 语法错误

```
Syntax error in SQL statement "DELETE FROM [*]user WHERE 1=1"; expected "identifier"
```

**原因**：`user` 是 H2 数据库的保留关键字，需要用反引号包裹。

#### 4.4 错误4：schema.sql 内容重复/损坏

```
IMESTAMP DEFAULT CURRENT_TIMESTAMP, `create_by` BIGINT DEFAULT 0...
```

**原因**：`schema.sql` 文件中存在残留代码片段 `);IMESTAMP...`，导致 SQL 解析失败。

#### 4.5 错误5：列数不匹配

```
Column count does not match; SQL statement: MERGE INTO `user` KEY(`id`) VALUES...
```

**原因**：`MERGE INTO` 语句的列数与表定义不匹配，H2 对 `MERGE` 语法要求严格。

---

### 五、根本原因总结

| 问题 | 严重程度 | 说明 |
| :--- | :--- | :--- |
| `schema.sql` 内容重复/损坏 | 🔴 严重 | 有残留代码 `);IMESTAMP...` |
| `user` 是 H2 保留关键字未转义 | 🔴 严重 | 需要用反引号包裹 |
| `MERGE INTO` 语法不兼容 H2 | 🟡 中等 | 应改用 `DELETE + INSERT` |
| `cleanup.sql` 文件缺失 | 🟡 中等 | 测试注解引用了不存在的文件 |
| `application-test.yml` 配置不完整 | 🟡 中等 | SQL 初始化模式配置问题 |

---

### 六、解决方案

#### 6.1 临时方案（已执行）

```bash
# 跳过测试，让主程序能运行
mvn clean package -DskipTests
mvn spring-boot:run
```

**结果**：主程序可以正常启动，登录接口可访问。

#### 6.2 根本解决方案（待执行）

1. **修复 schema.sql**
   - 删除重复内容和残留代码
   - 所有表名用反引号包裹

2. **修复 data.sql**
   - 移除 `MERGE INTO`，改用 `DELETE + INSERT`
   - 删除 `USE auth_system` 命令

3. **创建 cleanup.sql**
   ```sql
   DELETE FROM `user_role`;
   DELETE FROM `role_permission`;
   DELETE FROM `audit_log`;
   DELETE FROM `user`;
   DELETE FROM `role`;
   DELETE FROM `permission`;
   ```

4. **优化 application-test.yml**
   ```yaml
   spring:
     sql:
       init:
         mode: never
   ```

---

### 七、经验教训

| 教训 | 说明 |
| :--- | :--- |
| **测试环境应独立配置** | 测试环境的 SQL 脚本应与生产环境分开，使用 H2 兼容语法 |
| **H2 与 MySQL 语法差异** | `user` 是保留关键字、`MERGE` 语法不兼容、自增重置语法不同 |
| **SQL 脚本应保持简洁** | 测试脚本只包含 `CREATE TABLE` 和 `INSERT`，不包含 `USE`、`CREATE DATABASE` |
| **测试注解要完整** | 使用 `@Sql` 注解时，确保引用的脚本文件存在 |
| **先跑通主程序，再调试测试** | 测试环境问题不应阻塞主程序开发 |

---

### 八、当前状态

| 项目 | 状态 |
| :--- | :--- |
| 主程序 | ✅ 可正常启动 |
| 登录接口 | ✅ 可正常访问 |
| 单元测试 | ❌ 暂时跳过（`-DskipTests`） |

**后续计划**：主程序功能开发完成后，再回头修复测试环境配置。

---

### 九、附录：错误日志摘要

```
Tests run: 17, Failures: 0, Errors: 16, Skipped: 0
BUILD FAILURE

主要错误：
- CannotReadScriptException: cleanup.sql not found
- JdbcSQLSyntaxErrorException: Table "USER_ROLE" not found
- JdbcSQLSyntaxErrorException: Syntax error in SQL statement "DELETE FROM [*]user"
- JdbcSQLSyntaxErrorException: Column count does not match
```

---

**日期**：2026-04-21