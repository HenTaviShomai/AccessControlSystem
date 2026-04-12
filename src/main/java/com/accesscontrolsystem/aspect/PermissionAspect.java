package com.accesscontrolsystem.aspect;

import com.accesscontrolsystem.*;

import com.accesscontrolsystem.annotation.RequirePermission;
import com.accesscontrolsystem.enums.ErrorCode;
import com.accesscontrolsystem.exception.BusinessException;
import com.accesscontrolsystem.mapper.PermissionMapper;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.TimeUnit;

@Slf4j
@Aspect
@Component
public class PermissionAspect {

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @Autowired
    private PermissionMapper permissionMapper;

    private static final String USER_PERMISSION_KEY = "auth:user:permissions:";
    private static final long CACHE_EXPIRE_HOURS = 24;

    /**
     * 在方法执行前进行权限校验
     */
    @Before("@annotation(com.AccessControlSystem.annotation.RequirePermission)")
    public void checkPermission(JoinPoint joinPoint) {
        // 1. 获取当前登录用户ID
        Long userId = getCurrentUserId();
        if (userId == null) {
            log.warn("未获取到当前用户ID，可能未登录");
            throw new BusinessException(ErrorCode.NO_LOGIN);
        }

        // 2. 获取方法上的权限注解
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        RequirePermission requirePermission = method.getAnnotation(RequirePermission.class);

        if (requirePermission == null) {
            return;
        }

        // 3. 获取需要的权限列表
        String[] requiredPermissions = requirePermission.value();
        if (requiredPermissions.length == 0) {
            return;
        }

        // 4. 获取用户拥有的权限
        Set<String> userPermissions = getUserPermissions(userId);

        // 5. 校验权限
        boolean hasPermission = checkPermissions(userPermissions, requiredPermissions, requirePermission.logical());

        if (!hasPermission) {
            log.warn("权限不足: userId={}, required={}, logical={}, userPermissions={}",
                    userId, Arrays.toString(requiredPermissions), requirePermission.logical(), userPermissions);
            throw new BusinessException(ErrorCode.NO_PERMISSION);
        }

        log.debug("权限校验通过: userId={}, required={}", userId, Arrays.toString(requiredPermissions));
    }

    /**
     * 获取当前登录用户ID
     */
    private Long getCurrentUserId() {
        try {
            Object details = SecurityContextHolder.getContext().getAuthentication().getDetails();
            if (details instanceof Long) {
                return (Long) details;
            }
        } catch (Exception e) {
            log.debug("获取当前用户ID失败: {}", e.getMessage());
        }
        return null;
    }

    /**
     * 获取用户权限（带缓存）
     */
    private Set<String> getUserPermissions(Long userId) {
        String cacheKey = USER_PERMISSION_KEY + userId;

        // 1. 先从Redis缓存获取
        @SuppressWarnings("unchecked")
        Set<String> permissions = (Set<String>) redisTemplate.opsForValue().get(cacheKey);

        if (permissions != null && !permissions.isEmpty()) {
            log.debug("从缓存获取用户权限: userId={}, permissions={}", userId, permissions);
            return permissions;
        }

        // 2. 缓存未命中，从数据库加载
        permissions = loadUserPermissionsFromDb(userId);

        // 3. 存入缓存
        if (permissions != null && !permissions.isEmpty()) {
            redisTemplate.opsForValue().set(cacheKey, permissions, CACHE_EXPIRE_HOURS, TimeUnit.HOURS);
            log.debug("用户权限已缓存: userId={}, permissions={}", userId, permissions);
        }

        return permissions != null ? permissions : new HashSet<>();
    }

    /**
     * 从数据库加载用户权限
     */
    private Set<String> loadUserPermissionsFromDb(Long userId) {
        try {
            Set<String> permissions = permissionMapper.selectPermissionCodesByUserId(userId);
            log.debug("从数据库加载用户权限: userId={}, permissions={}", userId, permissions);
            return permissions;
        } catch (Exception e) {
            log.error("加载用户权限失败: userId={}", userId, e);
            return new HashSet<>();
        }
    }

    /**
     * 校验权限
     */
    private boolean checkPermissions(Set<String> userPermissions,
                                     String[] requiredPermissions,
                                     RequirePermission.Logical logical) {
        if (logical == RequirePermission.Logical.AND) {
            // AND：需要拥有所有权限
            return Arrays.stream(requiredPermissions)
                    .allMatch(userPermissions::contains);
        } else {
            // OR：只需要拥有其中一个权限
            return Arrays.stream(requiredPermissions)
                    .anyMatch(userPermissions::contains);
        }
    }

    /**
     * 清除用户权限缓存（权限变更时调用）
     */
    public void clearUserPermissionCache(Long userId) {
        String cacheKey = USER_PERMISSION_KEY + userId;
        redisTemplate.delete(cacheKey);
        log.info("清除用户权限缓存: userId={}", userId);
    }
}