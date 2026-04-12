package com.AccessControlSystem.service;

import java.util.List;
import java.util.Set;

public interface PermissionService {
    
    /**
     * 获取用户的所有权限码
     */
    Set<String> getUserPermissions(Long userId);
    
    /**
     * 刷新用户权限缓存（用户权限变更后调用）
     */
    void refreshUserPermissions(Long userId);
    
    /**
     * 批量刷新权限缓存（角色权限变更后调用）
     */
    void refreshPermissionsByRoleId(Long roleId);
}
