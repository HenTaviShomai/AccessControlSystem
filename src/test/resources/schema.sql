-- 用户表
    CREATE TABLE IF NOT EXISTS `user` (
                                          `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
                                          `username` VARCHAR(50) NOT NULL,
    `password` VARCHAR(100) NOT NULL,
    `nickname` VARCHAR(50),
    `status` TINYINT DEFAULT 1,
    `create_time` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    `update_time` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    `create_by` BIGINT DEFAULT 0,
    `update_by` BIGINT DEFAULT 0,
    `version` INT DEFAULT 0,
    `deleted` TINYINT DEFAULT 0
    );

-- 角色表
    CREATE TABLE IF NOT EXISTS `role` (
                                          `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
                                          `role_code` VARCHAR(50) NOT NULL,
    `role_name` VARCHAR(50) NOT NULL,
    `deleted` TINYINT DEFAULT 0,
    `create_time` TIMESTAMP DEFAULT CURRENT_TIMESTAMP
    );

-- 权限表
    CREATE TABLE IF NOT EXISTS `permission` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `permission_code` VARCHAR(100) NOT NULL,
    `permission_name` VARCHAR(50) NOT NULL,
    `parent_id` BIGINT DEFAULT 0,
    `sort` INT DEFAULT 0
    );

-- 用户-角色关联表
    CREATE TABLE IF NOT EXISTS `user_role` (
                                               `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
                                               `user_id` BIGINT NOT NULL,
                                               `role_id` BIGINT NOT NULL
                                           );

-- 角色-权限关联表
    CREATE TABLE IF NOT EXISTS `role_permission` (
                                                     `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
                                                     `role_id` BIGINT NOT NULL,
                                                     `permission_id` BIGINT NOT NULL
                                                 );

-- 审计日志表
    CREATE TABLE IF NOT EXISTS `audit_log` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `user_id` BIGINT,
    `username` VARCHAR(50),
    `operation` VARCHAR(100),
    `method` VARCHAR(200),
    `params` TEXT,
    `ip` VARCHAR(50),
    `result` VARCHAR(20),
    `error_msg` VARCHAR(500),
    `duration_ms` INT,
    `create_time` TIMESTAMP DEFAULT CURRENT_TIMESTAMP
    );IMESTAMP DEFAULT CURRENT_TIMESTAMP,
    `create_by` BIGINT DEFAULT 0,
    `update_by` BIGINT DEFAULT 0,
    `version` INT DEFAULT 0,
    `deleted` TINYINT DEFAULT 0
    );

-- 角色表
CREATE TABLE IF NOT EXISTS `role` (
                                      `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
                                      `role_code` VARCHAR(50) NOT NULL,
    `role_name` VARCHAR(50) NOT NULL,
    `deleted` TINYINT DEFAULT 0,
    `create_time` TIMESTAMP DEFAULT CURRENT_TIMESTAMP
    );

-- 权限表
CREATE TABLE IF NOT EXISTS `permission` (
                                            `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
                                            `permission_code` VARCHAR(100) NOT NULL,
    `permission_name` VARCHAR(50) NOT NULL,
    `parent_id` BIGINT DEFAULT 0,
    `sort` INT DEFAULT 0
    );

-- 用户-角色关联表
CREATE TABLE IF NOT EXISTS `user_role` (
                                           `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
                                           `user_id` BIGINT NOT NULL,
                                           `role_id` BIGINT NOT NULL
);

-- 角色-权限关联表
CREATE TABLE IF NOT EXISTS `role_permission` (
                                                 `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
                                                 `role_id` BIGINT NOT NULL,
                                                 `permission_id` BIGINT NOT NULL
);

-- 审计日志表
CREATE TABLE IF NOT EXISTS `audit_log` (
                                           `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
                                           `user_id` BIGINT,
                                           `username` VARCHAR(50),
    `operation` VARCHAR(100),
    `method` VARCHAR(200),
    `params` TEXT,
    `ip` VARCHAR(50),
    `result` VARCHAR(20),
    `error_msg` VARCHAR(500),
    `duration_ms` INT,
    `create_time` TIMESTAMP DEFAULT CURRENT_TIMESTAMP
    );