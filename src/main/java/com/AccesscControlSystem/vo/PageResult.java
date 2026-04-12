package com.AccesscControlSystem.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import java.util.List;

@Data
@AllArgsConstructor
public class PageResult<T> {
    private Long total;      // 总记录数
    private List<T> list;    // 当前页数据
}