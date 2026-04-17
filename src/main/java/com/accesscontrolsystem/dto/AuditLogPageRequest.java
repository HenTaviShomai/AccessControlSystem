package com.AccessControlSystem.dto;

import jakarta.validation.constraints.Min;
import lombok.Data;
import java.time.LocalDateTime;

@Data
public class AuditLogPageRequest {
    
    @Min(value = 1, message = "页码最小为1")
    private Integer pageNum = 1;
    
    @Min(value = 1, message = "每页条数最小为1")
    private Integer pageSize = 10;
    
    private Long userId;           // 按用户筛选
    private String username;       // 按用户名模糊查询
    private String operation;      // 按操作名称模糊查询
    private String result;         // 按结果筛选：SUCCESS/FAIL
    private LocalDateTime startTime;  // 开始时间
    private LocalDateTime endTime;    // 结束时间
}
