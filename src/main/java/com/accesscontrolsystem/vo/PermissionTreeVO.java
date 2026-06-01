package com.accesscontrolsystem.vo;

import lombok.Data;
import java.util.List;

@Data
public class PermissionTreeVO {
    private Long id;
    private String label;
    private String code;
    private List<PermissionTreeVO> children;
}
