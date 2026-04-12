package com.accesscontrolsystem.service.impl;

import com.accesscontrolsystem.dto.*;
import com.accesscontrolsystem.entity.Permission;
import com.accesscontrolsystem.entity.Role;
import com.accesscontrolsystem.entity.RolePermission;
import com.accesscontrolsystem.enums.ErrorCode;
import com.accesscontrolsystem.exception.BusinessException;
import com.accesscontrolsystem.mapper.PermissionMapper;
import com.accesscontrolsystem.mapper.RoleMapper;
import com.accesscontrolsystem.mapper.RolePermissionMapper;
import com.accesscontrolsystem.mapper.UserRoleMapper;
import com.accesscontrolsystem.service.PermissionService;
import com.accesscontrolsystem.service.RoleService;
import com.accesscontrolsystem.vo.PermissionTreeVO;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class RoleServiceImpl implements RoleService {
    
    private final RoleMapper roleMapper;
    private final PermissionMapper permissionMapper;
    private final RolePermissionMapper rolePermissionMapper;
    private final PermissionService permissionService;
    private final RedisTemplate<String, Object> redisTemplate;  // 新增
    private final UserRoleMapper userRoleMapper;
    private static final String USER_PERMISSION_KEY = "auth:user:permissions:";

    @Override
    public List<RoleResponse> list() {
        LambdaQueryWrapper<Role> wrapper = new LambdaQueryWrapper<>();
        wrapper.orderByAsc(Role::getRoleCode);
        
        List<Role> roles = roleMapper.selectList(wrapper);
        return roles.stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }
    
    @Override
    public RoleResponse getById(Long id) {
        Role role = roleMapper.selectById(id);
        if (role == null) {
            throw new BusinessException(ErrorCode.ROLE_NOT_FOUND);
        }
        return convertToResponse(role);
    }
    
    @Override
    @Transactional
    public void add(RoleRequest request) {
        // 检查角色代码是否已存在
        LambdaQueryWrapper<Role> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Role::getRoleCode, request.getRoleCode());
        if (roleMapper.selectCount(wrapper) > 0) {
            throw new BusinessException(ErrorCode.ROLE_CODE_EXISTS);
        }
        
        Role role = new Role();
        role.setRoleCode(request.getRoleCode());
        role.setRoleName(request.getRoleName());
        
        roleMapper.insert(role);
        log.info("新增角色成功: roleCode={}", role.getRoleCode());
    }
    
    @Override
    @Transactional
    public void update(RoleUpdateRequest request) {
        Role role = roleMapper.selectById(request.getId());
        if (role == null) {
            throw new BusinessException(ErrorCode.ROLE_NOT_FOUND);
        }
        
        if (StringUtils.hasText(request.getRoleCode())) {
            // 检查新代码是否与其他角色冲突
            LambdaQueryWrapper<Role> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(Role::getRoleCode, request.getRoleCode())
                   .ne(Role::getId, request.getId());
            if (roleMapper.selectCount(wrapper) > 0) {
                throw new BusinessException(ErrorCode.ROLE_CODE_EXISTS);
            }
            role.setRoleCode(request.getRoleCode());
        }
        
        if (StringUtils.hasText(request.getRoleName())) {
            role.setRoleName(request.getRoleName());
        }
        
        roleMapper.updateById(role);
        log.info("更新角色成功: roleId={}", role.getId());
    }
    
    @Override
    @Transactional
    public void delete(Long id) {
        // 检查角色是否存在
        Role role = roleMapper.selectById(id);
        if (role == null) {
            throw new BusinessException(ErrorCode.ROLE_NOT_FOUND);
        }
        
        // 逻辑删除角色
        roleMapper.deleteById(id);
        
        // 删除角色-权限关联
        rolePermissionMapper.deleteByRoleId(id);
        
        log.info("删除角色成功: roleId={}", id);
    }

    @Override
    @Transactional
    public void assignPermissions(AssignPermissionsRequest request) {
        Long roleId = request.getRoleId();

        // 检查角色是否存在
        Role role = roleMapper.selectById(roleId);
        if (role == null) {
            throw new BusinessException(ErrorCode.ROLE_NOT_FOUND);
        }

        // 删除原有权限关联
        rolePermissionMapper.deleteByRoleId(roleId);

        // 添加新的权限关联
        List<Long> permissionIds = request.getPermissionIds();
        if (permissionIds != null && !permissionIds.isEmpty()) {
            for (Long permissionId : permissionIds) {
                RolePermission rp = new RolePermission();
                rp.setRoleId(roleId);
                rp.setPermissionId(permissionId);
                rolePermissionMapper.insert(rp);
            }
        }
        clearUserPermissionCacheByRoleId(roleId);
        // 【新增】刷新该角色关联的所有用户的权限缓存
        permissionService.refreshPermissionsByRoleId(roleId);

        log.info("分配权限成功: roleId={}, permissionIds={}", roleId, permissionIds);
    }
    private void clearUserPermissionCacheByRoleId(Long roleId) {
        // 查询拥有该角色的所有用户ID

        List<Long> userIds = userRoleMapper.selectUserIdsByRoleId(roleId);
        for (Long userId : userIds) {
            String cacheKey = USER_PERMISSION_KEY + userId;
            redisTemplate.delete(cacheKey);
            log.info("清除用户权限缓存: userId={}", userId);
        }
    }
    @Override
    public List<PermissionTreeVO> getPermissionTree() {
        List<Permission> permissions = permissionMapper.selectAllPermissions();
        return buildTree(permissions, 0L);
    }
    
    /**
     * 构建权限树
     */
    private List<PermissionTreeVO> buildTree(List<Permission> permissions, Long parentId) {
        return permissions.stream()
                .filter(p -> p.getParentId().equals(parentId))
                .map(p -> {
                    PermissionTreeVO node = new PermissionTreeVO();
                    node.setId(p.getId());
                    node.setLabel(p.getPermissionName());
                    node.setCode(p.getPermissionCode());
                    node.setChildren(buildTree(permissions, p.getId()));
                    return node;
                })
                .collect(Collectors.toList());
    }
    
    /**
     * Role实体 → RoleResponse
     */
    private RoleResponse convertToResponse(Role role) {
        RoleResponse response = new RoleResponse();
        response.setId(role.getId());
        response.setRoleCode(role.getRoleCode());
        response.setRoleName(role.getRoleName());
        response.setCreateTime(role.getCreateTime());
        
        // 查询角色的权限码列表
        List<String> permissions = permissionMapper.selectPermissionCodesByRoleId(role.getId());
        response.setPermissions(permissions);
        
        return response;
    }
}
