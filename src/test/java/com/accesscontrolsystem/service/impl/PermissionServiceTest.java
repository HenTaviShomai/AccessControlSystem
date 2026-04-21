package com.accesscontrolsystem.service.impl;

import com.accesscontrolsystem.BaseTest;
import com.accesscontrolsystem.constant.RedisConstants;
import com.accesscontrolsystem.mapper.PermissionMapper;
import com.accesscontrolsystem.mapper.UserRoleMapper;
import com.accesscontrolsystem.service.PermissionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.SetOperations;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@DisplayName("权限服务测试")
class PermissionServiceTest extends BaseTest {
    
    @Autowired
    private PermissionService permissionService;
    
    @MockitoBean
    private UserRoleMapper userRoleMapper;
    
    @MockitoBean
    private PermissionMapper permissionMapper;
    
    @MockitoBean
    private RedisTemplate<String, String> redisTemplate;
    
    @MockitoBean
    private SetOperations<String, String> setOperations;
    
    @BeforeEach
    void setUp() {
        when(redisTemplate.opsForSet()).thenReturn(setOperations);
    }
    
    @Test
    @DisplayName("获取用户权限 - 从缓存获取")
    void testGetUserPermissionsFromCache() {
        Long userId = 1L;
        String cacheKey = RedisConstants.USER_PERMISSIONS_KEY + userId;
        Set<String> cachedPermissions = Set.of("user:list", "user:add");
        
        when(setOperations.members(cacheKey)).thenReturn(cachedPermissions);
        
        Set<String> permissions = permissionService.getUserPermissions(userId);
        
        assertNotNull(permissions);
        assertEquals(2, permissions.size());
        assertTrue(permissions.contains("user:list"));
        assertTrue(permissions.contains("user:add"));
        
        // 验证没有查数据库
        verify(userRoleMapper, never()).selectRoleIdsByUserId(any());
    }
    
    @Test
    @DisplayName("获取用户权限 - 缓存未命中，从数据库加载")
    void testGetUserPermissionsFromDB() {
        Long userId = 1L;
        String cacheKey = RedisConstants.USER_PERMISSIONS_KEY + userId;
        List<Long> roleIds = List.of(1L, 2L);
        List<String> permissionCodes = List.of("user:list", "user:add", "user:delete");
        
        when(setOperations.members(cacheKey)).thenReturn(Set.of());  // 缓存为空
        when(userRoleMapper.selectRoleIdsByUserId(userId)).thenReturn(roleIds);
        when(permissionMapper.selectPermissionCodesByRoleId(1L)).thenReturn(List.of("user:list", "user:add"));
        when(permissionMapper.selectPermissionCodesByRoleId(2L)).thenReturn(List.of("user:delete"));
        
        Set<String> permissions = permissionService.getUserPermissions(userId);
        
        assertNotNull(permissions);
        assertEquals(3, permissions.size());
        
        // 验证缓存写入
        verify(setOperations, times(3)).add(eq(cacheKey), anyString());
    }
    
    @Test
    @DisplayName("刷新用户权限缓存")
    void testRefreshUserPermissions() {
        Long userId = 1L;
        String cacheKey = RedisConstants.USER_PERMISSIONS_KEY + userId;
        
        permissionService.refreshUserPermissions(userId);
        
        verify(redisTemplate, times(1)).delete(cacheKey);
    }
}
