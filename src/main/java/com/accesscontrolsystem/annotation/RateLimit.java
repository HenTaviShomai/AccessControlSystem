package com.accesscontrolsystem.annotation;

import java.lang.annotation.*;
import java.util.concurrent.TimeUnit;

/**
 * 限流注解
 * 使用方式：@RateLimit(key = "login", permitsPerSecond = 5)
 */
@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface RateLimit {
    
    /**
     * 限流的key（用于区分不同接口）
     */
    String key();
    
    /**
     * 每秒允许的请求数
     */
    double permitsPerSecond() default 10.0;
    
    /**
     * 超时时间（秒）
     */
    int timeout() default 1;
    
    /**
     * 超时时间单位
     */
    TimeUnit timeUnit() default TimeUnit.SECONDS;
}
