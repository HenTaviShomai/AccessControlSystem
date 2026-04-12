package com.accessccontrolsystem.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class RoleRequest {

    @NotBlank(message = "角色代码不能为空")
    private String roleCode;

    @NotBlank(message = "角色名称不能为空")
    private String roleName;
}