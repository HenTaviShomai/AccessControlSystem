package com.AccesscControlSystem.dto;

import jakarta.validation.constraints.Min;
import lombok.Data;

@Data
public class UserPageRequest {

    @Min(value = 1, message = "页码最小为1")
    private Integer pageNum = 1;

    @Min(value = 1, message = "每页条数最小为1")
    private Integer pageSize = 10;

    private String username;   // 模糊查询
    private Integer status;    // 状态筛选
}