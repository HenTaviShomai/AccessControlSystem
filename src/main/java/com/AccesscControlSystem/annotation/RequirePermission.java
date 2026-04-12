package com.AccesscControlSystem.annotation;

import java.lang.annotation.*;

/**
 * 自定义权限校验注解
 * 使用方式：@RequirePermission("user:list")
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface RequirePermission {

    /**
     * 需要的权限码
     */
    String[] value() default {};

    /**
     * 逻辑关系：AND（需要所有权限）或 OR（只需要其中一个）
     */
    Logical logical() default Logical.AND;

    enum Logical {
        AND, OR
    }
}