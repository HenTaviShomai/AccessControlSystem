package com.accessccontrolsystem.vo;

import lombok.Data;
import java.util.List;

@Data
public class PermissionTreeVO {
    private Long id;
    private String label;      // 显示名称
    private String code;       // 权限码
    private List<PermissionTreeVO> children;
}
