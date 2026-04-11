package com.AccessControlSystem.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;
import java.util.List;

@Data
public class AssignPermissionsRequest {
    
    @NotNull(message = "角色ID不能为空")
    private Long roleId;
    
    private List<Long> permissionIds;  // 要分配的权限ID列表
}
