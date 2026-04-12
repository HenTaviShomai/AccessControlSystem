package com.accesscontrolsystem.service;

import com.accesscontrolsystem.dto.AssignRolesRequest;
import java.util.List;

public interface UserRoleService {
    
    void assignRoles(AssignRolesRequest request);
    
    List<Long> getUserRoleIds(Long userId);
}
