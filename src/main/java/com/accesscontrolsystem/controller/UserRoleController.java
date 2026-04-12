package com.accesscontrolsystem.controller;

import com.accesscontrolsystem.dto.AssignRolesRequest;
import com.accesscontrolsystem.service.UserRoleService;
import com.accesscontrolsystem.vo.Result;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/user-role")
@RequiredArgsConstructor
public class UserRoleController {
    
    private final UserRoleService userRoleService;
    
    /**
     * 给用户分配角色
     * 权限：user:edit
     */
    @PostMapping("/assign")
    @PreAuthorize("hasAuthority('user:edit')")
    public Result<Void> assignRoles(@Valid @RequestBody AssignRolesRequest request) {
        userRoleService.assignRoles(request);
        return Result.success();
    }
    
    /**
     * 获取用户的角色ID列表
     * 权限：user:list
     */
    @GetMapping("/user/{userId}")
    @PreAuthorize("hasAuthority('user:list')")
    public Result<java.util.List<Long>> getUserRoles(@PathVariable Long userId) {
        java.util.List<Long> roleIds = userRoleService.getUserRoleIds(userId);
        return Result.success(roleIds);
    }
}
