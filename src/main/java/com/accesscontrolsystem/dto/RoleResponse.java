package com.accesscontrolsystem.dto;

import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class RoleResponse {
    private Long id;
    private String roleCode;
    private String roleName;
    private List<String> permissions;  // 权限码列表
    private LocalDateTime createTime;
}