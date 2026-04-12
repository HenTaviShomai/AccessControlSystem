package com.accessccontrolsystem.service;

import com.accessccontrolsystem.dto.AssignRolesRequest;
import java.util.List;

public interface UserRoleService {
    
    void assignRoles(AssignRolesRequest request);
    
    List<Long> getUserRoleIds(Long userId);
}
