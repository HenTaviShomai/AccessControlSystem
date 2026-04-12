package com.AccessControlSystem.service.impl;

import com.AccessControlSystem.constant.RedisConstants;
import com.AccessControlSystem.entity.Role;
import com.AccessControlSystem.mapper.PermissionMapper;
import com.AccessControlSystem.mapper.RoleMapper;
import com.AccessControlSystem.mapper.UserRoleMapper;
import com.AccessControlSystem.service.PermissionService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class PermissionServiceImpl implements PermissionService {
    
    private final UserRoleMapper userRoleMapper;
    private final RoleMapper roleMapper;
    private final PermissionMapper permissionMapper;
    private final RedisTemplate<String, String> redisTemplate;
    
    @Override
    public Set<String> getUserPermissions(Long userId) {
        String cacheKey = RedisConstants.USER_PERMISSIONS_KEY + userId;
        
        // 1. 先从Redis缓存获取
        Set<String> cachedPermissions = redisTemplate.opsForSet().members(cacheKey);
        if (cachedPermissions != null && !cachedPermissions.isEmpty()) {
            log.debug("从缓存获取用户权限: userId={}, permissions={}", userId, cachedPermissions);
            return cachedPermissions;
        }
        
        // 2. 缓存未命中，从数据库查询
        Set<String> permissions = loadUserPermissionsFromDB(userId);
        
        // 3. 写入缓存
        if (!permissions.isEmpty()) {
            for (String permission : permissions) {
                redisTemplate.opsForSet().add(cacheKey, permission);
            }
            redisTemplate.expire(cacheKey, RedisConstants.PERMISSIONS_EXPIRE_SECONDS, TimeUnit.SECONDS);
        }
        
        log.info("从数据库加载用户权限: userId={}, permissions={}", userId, permissions);
        return permissions;
    }
    
    @Override
    public void refreshUserPermissions(Long userId) {
        String cacheKey = RedisConstants.USER_PERMISSIONS_KEY + userId;
        redisTemplate.delete(cacheKey);
        log.info("刷新用户权限缓存: userId={}", userId);
    }
    
    @Override
    public void refreshPermissionsByRoleId(Long roleId) {
        // 查询拥有该角色的所有用户
        List<Long> userIds = userRoleMapper.selectUserIdsByRoleId(roleId);
        for (Long userId : userIds) {
            refreshUserPermissions(userId);
        }
        log.info("刷新角色关联的用户权限缓存: roleId={}, userIds={}", roleId, userIds);
    }
    
    /**
     * 从数据库加载用户权限
     */
    private Set<String> loadUserPermissionsFromDB(Long userId) {
        // 1. 查询用户的所有角色
        List<Long> roleIds = userRoleMapper.selectRoleIdsByUserId(userId);
        if (roleIds.isEmpty()) {
            return new HashSet<>();
        }
        
        // 2. 查询角色的所有权限码
        return roleIds.stream()
                .flatMap(roleId -> permissionMapper.selectPermissionCodesByRoleId(roleId).stream())
                .collect(Collectors.toSet());
    }
}
