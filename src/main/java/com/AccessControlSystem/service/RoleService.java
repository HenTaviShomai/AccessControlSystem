package com.AccessControlSystem.service;

import com.AccessControlSystem.dto.*;
import com.AccessControlSystem.vo.PermissionTreeVO;

import java.util.List;

public interface RoleService {
    
    List<RoleResponse> list();
    
    RoleResponse getById(Long id);
    
    void add(RoleRequest request);
    
    void update(RoleUpdateRequest request);
    
    void delete(Long id);
    
    void assignPermissions(AssignPermissionsRequest request);
    
    List<PermissionTreeVO> getPermissionTree();
}
