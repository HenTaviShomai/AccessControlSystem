package com.accessccontrolsystem.enums;


import lombok.Getter;

@Getter
public enum ErrorCode {

    SUCCESS(0, "成功"),
    SYSTEM_ERROR(500, "系统繁忙，请稍后重试"),

    // 参数错误 1000-1999
    PARAM_ERROR(1001, "参数错误"),
    PARAM_MISSING(1002, "缺少必要参数"),
    PARAM_INVALID(1003, "参数格式错误"),

    // 认证错误 2000-2999
    TOKEN_MISSING(2001, "缺少Token"),
    TOKEN_INVALID(2002, "无效Token"),
    TOKEN_EXPIRED(2003, "Token已过期"),
    LOGIN_EXPIRED(2004, "登录已过期，请重新登录"),

    // 权限错误 3000-3999
    NO_PERMISSION(3001, "无操作权限"),
    NO_LOGIN(3002, "请先登录"),

    // 业务错误 4000-4999
    USER_NOT_FOUND(4001, "用户不存在"),
    PASSWORD_ERROR(4002, "密码错误"),
    USER_DISABLED(4003, "账号已被禁用"),
    USERNAME_EXISTS(4004, "用户名已存在"),
    ROLE_NOT_FOUND(4005, "角色不存在"),
    PERMISSION_NOT_FOUND(4006, "权限不存在"),
    REQUEST_TOO_FREQUENT(4007, "请求过于频繁，请稍后重试"),
    REPEAT_SUBMIT(4008, "请勿重复提交"),
    ROLE_CODE_EXISTS(4009, "角色代码已存在");

    private final Integer code;
    private final String msg;

    ErrorCode(Integer code, String msg) {
        this.code = code;
        this.msg = msg;
    }
}