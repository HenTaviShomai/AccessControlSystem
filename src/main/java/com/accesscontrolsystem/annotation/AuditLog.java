package com.accesscontrolsystem.annotation;

import java.lang.annotation.*;


@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface AuditLog {
    

    String value();
    

    boolean recordParams() default true;
    

    boolean recordResult() default false;
}
