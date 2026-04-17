package com.accesscontrolsystem.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("audit_log")
public class AuditLog extends BaseEntity {
    
    private Long userId;           // 操作用户ID
    private String username;       // 操作用户名（冗余，防止用户被删后查不到）
    private String operation;      // 操作名称：用户登录、删除用户
    private String method;         // 请求方法+URL：DELETE /user/1
    private String params;         // 请求参数（已脱敏）
    private String ip;             // 客户端IP
    private String result;         // 结果：SUCCESS/FAIL
    private String errorMsg;       // 错误信息
    private Integer durationMs;    // 执行耗时（毫秒）
    private LocalDateTime createTime;
}
