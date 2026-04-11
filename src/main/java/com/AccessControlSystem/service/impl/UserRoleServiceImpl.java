package com.AccessControlSystem.service.impl;

import com.AccessControlSystem.dto.AssignRolesRequest;
import com.AccessControlSystem.entity.User;
import com.AccessControlSystem.entity.UserRole;
import com.AccessControlSystem.enums.ErrorCode;
import com.AccessControlSystem.exception.BusinessException;
import com.AccessControlSystem.mapper.UserMapper;
import com.AccessControlSystem.mapper.UserRoleMapper;
import com.AccessControlSystem.service.UserRoleService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserRoleServiceImpl implements UserRoleService {
    
    private final UserMapper userMapper;
    private final UserRoleMapper userRoleMapper;
    
    @Override
    @Transactional
    public void assignRoles(AssignRolesRequest request) {
        Long userId = request.getUserId();
        
        // 检查用户是否存在
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND);
        }
        
        // 删除原有角色关联
        userRoleMapper.deleteByUserId(userId);
        
        // 添加新的角色关联
        List<Long> roleIds = request.getRoleIds();
        if (roleIds != null && !roleIds.isEmpty()) {
            for (Long roleId : roleIds) {
                UserRole ur = new UserRole();
                ur.setUserId(userId);
                ur.setRoleId(roleId);
                userRoleMapper.insert(ur);
            }
        }
        
        log.info("分配角色成功: userId={}, roleIds={}", userId, roleIds);
    }
    
    @Override
    public List<Long> getUserRoleIds(Long userId) {
        return userRoleMapper.selectRoleIdsByUserId(userId);
    }
}
