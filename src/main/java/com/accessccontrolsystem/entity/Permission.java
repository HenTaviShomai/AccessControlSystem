package com.accessccontrolsystem.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("permission")
public class Permission extends BaseEntity {
    private String permissionCode;
    private String permissionName;
    private Long parentId;
    private Integer sort;
}