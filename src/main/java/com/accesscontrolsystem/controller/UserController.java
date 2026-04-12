package com.accesscontrolsystem.controller;

import com.accesscontrolsystem.annotation.RequirePermission;
import com.accesscontrolsystem.dto.UserPageRequest;
import com.accesscontrolsystem.dto.UserRequest;
import com.accesscontrolsystem.dto.UserResponse;
import com.accesscontrolsystem.dto.UserUpdateRequest;
import com.accesscontrolsystem.service.UserService;
import com.accesscontrolsystem.vo.PageResult;
import com.accesscontrolsystem.vo.Result;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/user")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;
    /**
     * 只需要其中一个权限（OR关系）
     */
    @GetMapping("/export")
    @RequirePermission(value = {"user:export", "admin:all"}, logical = RequirePermission.Logical.OR)
    public Result<Void> export() {
        // 拥有user:export或admin:all任意一个即可
        return Result.success();
    }

    /**
     * 分页查询用户列表
     * 权限：user:list
     */
    @GetMapping("/page")
    @PreAuthorize("hasAuthority('user:list')")
    public Result<PageResult<UserResponse>> pageList(@Valid UserPageRequest request) {
        PageResult<UserResponse> result = userService.pageList(request);
        return Result.success(result);
    }

    /**
     * 查询用户详情
     * 权限：user:list
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('user:list')")
    public Result<UserResponse> getById(@PathVariable Long id) {
        UserResponse user = userService.getById(id);
        return Result.success(user);
    }

    /**
     * 新增用户
     * 权限：user:add
     */
    @PostMapping
    @PreAuthorize("hasAuthority('user:add')")
    public Result<Void> add(@Valid @RequestBody UserRequest request) {
        userService.add(request);
        return Result.success();
    }

    /**
     * 修改用户
     * 权限：user:edit
     */
    @PutMapping
    @PreAuthorize("hasAuthority('user:edit')")
    public Result<Void> update(@Valid @RequestBody UserUpdateRequest request) {
        userService.update(request);
        return Result.success();
    }

    /**
     * 删除用户
     * 权限：user:delete
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('user:delete')")
    public Result<Void> delete(@PathVariable Long id) {
        userService.delete(id);
        return Result.success();
    }

    /**
     * 需要多个权限（AND关系）
     */
    @PostMapping("/batch-delete")
    @RequirePermission(value = {"user:list", "user:delete"}, logical = RequirePermission.Logical.AND)
    public Result<Void> batchDelete(@RequestBody List<Long> ids) {
        // 需要同时拥有user:list和user:delete权限
        return Result.success();
    }

    /**
     * 修改用户状态（启用/禁用）
     * 权限：user:edit
     */
    @PutMapping("/{id}/status")
    @PreAuthorize("hasAuthority('user:edit')")
    public Result<Void> updateStatus(@PathVariable Long id, @RequestParam Integer status) {
        userService.updateStatus(id, status);
        return Result.success();
    }
}