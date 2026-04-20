package com.accesscontrolsystem.annotation;

import java.lang.annotation.*;
import java.util.concurrent.TimeUnit;

/**
 * 防重复提交注解
 * 使用方式：@NoRepeatSubmit(duration = 3)
 */
@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface NoRepeatSubmit {
    
    /**
     * 锁定的时间（秒），在这个时间内相同参数的请求只处理一次
     */
    long duration() default 3;
    
    /**
     * 时间单位
     */
    TimeUnit timeUnit() default TimeUnit.SECONDS;
}
