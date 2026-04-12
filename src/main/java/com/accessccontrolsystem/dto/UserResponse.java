package com.accessccontrolsystem.dto;

import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class UserResponse {
    private Long id;
    private String username;
    private String nickname;
    private Integer status;
    private List<String> roles;  // 角色代码列表
    private LocalDateTime createTime;
}