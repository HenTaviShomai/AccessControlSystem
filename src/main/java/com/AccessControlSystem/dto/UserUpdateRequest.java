package com.AccessControlSystem.dto;


import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class UserUpdateRequest {

    @NotNull(message = "用户ID不能为空")
    private Long id;

    private String nickname;
    private Integer status;
    private String password;  // 可选，不填则不修改密码
}