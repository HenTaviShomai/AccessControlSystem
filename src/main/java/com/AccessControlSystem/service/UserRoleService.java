package com.AccessControlSystem.service;

import com.AccessControlSystem.dto.AssignRolesRequest;
import java.util.List;

public interface UserRoleService {
    
    void assignRoles(AssignRolesRequest request);
    
    List<Long> getUserRoleIds(Long userId);
}
