package com.AccessControlSystem.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;
import java.util.List;

@Data
public class AssignRolesRequest {
    
    @NotNull(message = "用户ID不能为空")
    private Long userId;
    
    private List<Long> roleIds;  // 要分配的角色ID列表
}
