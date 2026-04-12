package com.AccesscControlSystem.service;

import com.AccesscControlSystem.dto.AssignRolesRequest;
import java.util.List;

public interface UserRoleService {
    
    void assignRoles(AssignRolesRequest request);
    
    List<Long> getUserRoleIds(Long userId);
}
