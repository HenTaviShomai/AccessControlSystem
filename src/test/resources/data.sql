-- 删除所有数据
DELETE FROM `user_role`;
DELETE FROM `role_permission`;
DELETE FROM `audit_log`;
DELETE FROM `user`;
DELETE FROM `role`;
DELETE FROM `permission`;

-- 重置自增序列
ALTER TABLE `user` ALTER COLUMN id RESTART WITH 1;
ALTER TABLE `role` ALTER COLUMN id RESTART WITH 1;
ALTER TABLE `permission` ALTER COLUMN id RESTART WITH 1;

-- 插入用户
INSERT INTO `user` (username, password, nickname, status, create_time, update_time, deleted)
VALUES
    ('admin', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EfMlqYy7JkI3q5q5q5q5q', '管理员', 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0),
    ('user', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EfMlqYy7JkI3q5q5q5q5q', '普通用户', 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0);

-- 插入角色
INSERT INTO `role` (role_code, role_name, deleted, create_time)
VALUES
    ('ADMIN', '管理员', 0, CURRENT_TIMESTAMP),
    ('USER', '普通用户', 0, CURRENT_TIMESTAMP);

-- 插入权限
INSERT INTO `permission` (permission_code, permission_name, parent_id, sort)
VALUES
    ('user:list', '查询用户列表', 0, 0),
    ('user:add', '新增用户', 0, 0),
    ('user:edit', '编辑用户', 0, 0),
    ('user:delete', '删除用户', 0, 0),
    ('role:list', '查询角色列表', 0, 10),
    ('role:add', '添加角色', 0, 11),
    ('role:edit', '编辑角色', 0, 12),
    ('role:delete', '删除角色', 0, 13),
    ('log:list', '查询日志列表', 0, 20),
    ('log:export', '导出日志', 0, 21);

-- 给管理员分配所有权限
INSERT INTO `role_permission` (role_id, permission_id)
SELECT 1, id FROM `permission`;

-- 给普通用户分配查询权限
INSERT INTO `role_permission` (role_id, permission_id)
SELECT 2, id FROM `permission` WHERE permission_code IN ('user:list');

-- 给用户分配角色
INSERT INTO `user_role` (user_id, role_id) VALUES (1, 1), (2, 2);