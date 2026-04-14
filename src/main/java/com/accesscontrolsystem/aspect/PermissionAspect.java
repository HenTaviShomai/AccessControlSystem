package com.accesscontrolsystem.aspect;

import com.accesscontrolsystem.annotation.*;
import com.accesscontrolsystem.enums.ErrorCode;
import com.accesscontrolsystem.exception.BusinessException;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collection;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Aspect
@Component
public class PermissionAspect {
    
    /**
     * 在方法执行前进行权限校验
     */
    @Before("@annotation(com.AccessControlSystem.annotation.RequirePermission)")
    public void checkPermission(JoinPoint joinPoint) {
        // 1. 获取方法上的注解
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        com.AccessControlSystem.annotation.RequirePermission requirePermission = method.getAnnotation(RequirePermission.class);
        
        if (requirePermission == null) {
            return;
        }
        
        // 2. 获取需要的权限码列表
        String[] requiredPermissions = requirePermission.value();
        com.AccessControlSystem.annotation.RequirePermission.Logical logical = requirePermission.logical();
        
        // 3. 获取当前登录用户的权限列表
        Set<String> userPermissions = getCurrentUserPermissions();
        
        // 4. 校验权限
        boolean hasPermission = check(userPermissions, requiredPermissions, logical);
        
        if (!hasPermission) {
            log.warn("权限不足: 用户权限={}, 需要权限={}, 逻辑={}", 
                     userPermissions, Arrays.toString(requiredPermissions), logical);
            throw new BusinessException(ErrorCode.NO_PERMISSION);
        }
        
        log.debug("权限校验通过: 需要权限={}, 逻辑={}", Arrays.toString(requiredPermissions), logical);
    }
    
    /**
     * 获取当前用户的权限码集合
     */
    private Set<String> getCurrentUserPermissions() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            log.warn("用户未登录");
            return Set.of();
        }
        
        Collection<? extends GrantedAuthority> authorities = authentication.getAuthorities();
        return authorities.stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toSet());
    }
    
    /**
     * 权限校验逻辑
     */
    private boolean check(Set<String> userPermissions, String[] required, RequirePermission.Logical logical) {
        if (required.length == 0) {
            return true;
        }
        
        if (logical == RequirePermission.Logical.AND) {
            // AND：需要拥有所有权限
            return Arrays.stream(required).allMatch(userPermissions::contains);
        } else {
            // OR：只需要拥有任一权限
            return Arrays.stream(required).anyMatch(userPermissions::contains);
        }
    }
}
