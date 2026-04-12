package com.accesscontrolsystem.entity;


import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class User extends BaseEntity {

    private String username;
    private String password;
    private String nickname;
    private Integer status;  // 1正常 0禁用
}