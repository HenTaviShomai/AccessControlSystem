-- 创建测试表
CREATE TABLE IF NOT EXISTS `user` (
                                      `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
                                      `username` VARCHAR(50) NOT NULL,
    `password` VARCHAR(100) NOT NULL,
    `nickname` VARCHAR(50),
    `status` TINYINT DEFAULT 1,
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `deleted` TINYINT DEFAULT 0
    );

CREATE TABLE IF NOT EXISTS `role` (
                                      `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
                                      `role_code` VARCHAR(50) NOT NULL,
    `role_name` VARCHAR(50) NOT NULL,
    `deleted` TINYINT DEFAULT 0,
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP
    );

CREATE TABLE IF NOT EXISTS `permission` (
                                            `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
                                            `permission_code` VARCHAR(100) NOT NULL,
    `permission_name` VARCHAR(50) NOT NULL,
    `parent_id` BIGINT DEFAULT 0,
    `sort` INT DEFAULT 0
    );