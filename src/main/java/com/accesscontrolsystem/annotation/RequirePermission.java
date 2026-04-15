package com.accesscontrolsystem.annotation;

import java.lang.annotation.*;

/**
 * 权限校验注解
 * 使用方式：@RequirePermission("user:list")
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface RequirePermission {
    
    /**
     * 需要的权限码
     */
    String[] value();
    
    /**
     * 逻辑关系：AND（需要所有权限）或 OR（需要任一权限）
     */
    Logical logical() default Logical.AND;
    
    /**
     * 逻辑枚举
     */
    enum Logical {
        AND, OR
    }
}
