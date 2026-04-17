package com.AccessControlSystem.annotation;

import java.lang.annotation.*;

/**
 * 审计日志注解
 * 使用方式：@AuditLog("删除用户")
 */
@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface AuditLog {
    
    /**
     * 操作名称
     */
    String value();
    
    /**
     * 是否记录请求参数（默认true）
     */
    boolean recordParams() default true;
    
    /**
     * 是否记录返回值（默认false，因为返回值可能很大）
     */
    boolean recordResult() default false;
}
