-- 创建数据库
CREATE DATABASE IF NOT EXISTS access_control CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE access_control;

-- 用户表
CREATE TABLE IF NOT EXISTS `user` (
                                      `id` BIGINT NOT NULL AUTO_INCREMENT,
                                      `username` VARCHAR(50) NOT NULL COMMENT '登录名',
    `password` VARCHAR(100) NOT NULL COMMENT 'BCrypt加密密码',
    `nickname` VARCHAR(50) COMMENT '昵称',
    `status` TINYINT DEFAULT 1 COMMENT '1正常 0禁用',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `create_by` BIGINT DEFAULT 0,
    `update_by` BIGINT DEFAULT 0,
    `version` INT DEFAULT 0,
    `deleted` TINYINT DEFAULT 0,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_username` (`username`)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户表';

-- 角色表
CREATE TABLE IF NOT EXISTS `role` (
                                      `id` BIGINT NOT NULL AUTO_INCREMENT,
                                      `role_code` VARCHAR(50) NOT NULL COMMENT '角色代码',
    `role_name` VARCHAR(50) NOT NULL COMMENT '角色名称',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `deleted` TINYINT DEFAULT 0,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_role_code` (`role_code`)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='角色表';

-- 权限表
CREATE TABLE IF NOT EXISTS `permission` (
                                            `id` BIGINT NOT NULL AUTO_INCREMENT,
                                            `permission_code` VARCHAR(100) NOT NULL COMMENT '权限码',
    `permission_name` VARCHAR(50) NOT NULL COMMENT '权限名称',
    `parent_id` BIGINT DEFAULT 0 COMMENT '父权限ID',
    `sort` INT DEFAULT 0 COMMENT '排序',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_permission_code` (`permission_code`)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='权限表';

-- 用户-角色关联表
CREATE TABLE IF NOT EXISTS `user_role` (
                                           `id` BIGINT NOT NULL AUTO_INCREMENT,
                                           `user_id` BIGINT NOT NULL,
                                           `role_id` BIGINT NOT NULL,
                                           PRIMARY KEY (`id`),
    KEY `idx_user_id` (`user_id`),
    KEY `idx_role_id` (`role_id`)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户角色关联表';

-- 角色-权限关联表
CREATE TABLE IF NOT EXISTS `role_permission` (
                                                 `id` BIGINT NOT NULL AUTO_INCREMENT,
                                                 `role_id` BIGINT NOT NULL,
                                                 `permission_id` BIGINT NOT NULL,
                                                 PRIMARY KEY (`id`),
    KEY `idx_role_id` (`role_id`),
    KEY `idx_permission_id` (`permission_id`)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='角色权限关联表';

-- 审计日志表
CREATE TABLE IF NOT EXISTS `audit_log` (
                                           `id` BIGINT NOT NULL AUTO_INCREMENT,
                                           `user_id` BIGINT COMMENT '操作用户ID',
                                           `username` VARCHAR(50) COMMENT '操作用户名',
    `operation` VARCHAR(100) COMMENT '操作名称',
    `method` VARCHAR(200) COMMENT '请求方法+URL',
    `params` TEXT COMMENT '请求参数',
    `ip` VARCHAR(50) COMMENT '客户端IP',
    `result` VARCHAR(20) COMMENT '结果',
    `error_msg` VARCHAR(500) COMMENT '错误信息',
    `duration_ms` INT COMMENT '执行耗时',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    KEY `idx_user_id` (`user_id`),
    KEY `idx_create_time` (`create_time`)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='审计日志表';

-- 插入初始数据
INSERT INTO `role` (`role_code`, `role_name`) VALUES
                                                  ('ADMIN', '管理员'),
                                                  ('USER', '普通用户')
    ON DUPLICATE KEY UPDATE role_name = VALUES(role_name);

INSERT INTO `permission` (`permission_code`, `permission_name`, `parent_id`, `sort`) VALUES
                                                                                         ('user:list', '查询用户列表', 0, 1),
                                                                                         ('user:add', '新增用户', 0, 2),
                                                                                         ('user:edit', '编辑用户', 0, 3),
                                                                                         ('user:delete', '删除用户', 0, 4),
                                                                                         ('role:list', '查询角色列表', 0, 5),
                                                                                         ('role:add', '新增角色', 0, 6),
                                                                                         ('role:edit', '编辑角色', 0, 7),
                                                                                         ('role:delete', '删除角色', 0, 8),
                                                                                         ('log:list', '查询日志列表', 0, 9),
                                                                                         ('log:export', '导出日志', 0, 10)
    ON DUPLICATE KEY UPDATE permission_name = VALUES(permission_name);

-- 给管理员分配所有权限
INSERT INTO `role_permission` (`role_id`, `permission_id`)
SELECT 1, id FROM `permission`
    ON DUPLICATE KEY UPDATE permission_id = VALUES(permission_id);

-- 给普通用户分配查询权限
INSERT INTO `role_permission` (`role_id`, `permission_id`)
SELECT 2, id FROM `permission` WHERE permission_code IN ('user:list', 'role:list')
    ON DUPLICATE KEY UPDATE permission_id = VALUES(permission_id);

-- 插入测试用户（密码都是 123456）
INSERT INTO `user` (`username`, `password`, `nickname`, `status`) VALUES
                                                                      ('admin', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EfMlqYy7JkI3q5q5q5q5q', '管理员', 1),
                                                                      ('user', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EfMlqYy7JkI3q5q5q5q5q', '普通用户', 1)
    ON DUPLICATE KEY UPDATE username = VALUES(username);

-- 给用户分配角色
INSERT INTO `user_role` (`user_id`, `role_id`) VALUES
                                                   (1, 1),
                                                   (2, 2)
    ON DUPLICATE KEY UPDATE role_id = VALUES(role_id);