package com.AccesscControlSystem.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("role_permission")
public class RolePermission {
    private Long id;
    private Long roleId;
    private Long permissionId;
}
