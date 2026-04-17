package com.AccessControlSystem.dto;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class AuditLogResponse {
    private Long id;
    private Long userId;
    private String username;
    private String operation;
    private String method;
    private String params;
    private String ip;
    private String result;
    private String errorMsg;
    private Integer durationMs;
    private LocalDateTime createTime;
}
