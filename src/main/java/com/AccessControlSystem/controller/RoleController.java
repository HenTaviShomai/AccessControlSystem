package com.AccessControlSystem.controller;

import com.AccessControlSystem.dto.*;
import com.AccessControlSystem.service.RoleService;
import com.AccessControlSystem.vo.PermissionTreeVO;
import com.AccessControlSystem.vo.Result;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/role")
@RequiredArgsConstructor
public class RoleController {
    
    private final RoleService roleService;
    
    /**
     * 角色列表
     * 权限：role:list
     */
    @GetMapping("/list")
    @PreAuthorize("hasAuthority('role:list')")
    public Result<List<RoleResponse>> list() {
        List<RoleResponse> roles = roleService.list();
        return Result.success(roles);
    }
    
    /**
     * 角色详情
     * 权限：role:list
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('role:list')")
    public Result<RoleResponse> getById(@PathVariable Long id) {
        RoleResponse role = roleService.getById(id);
        return Result.success(role);
    }
    
    /**
     * 新增角色
     * 权限：role:add
     */
    @PostMapping
    @PreAuthorize("hasAuthority('role:add')")
    public Result<Void> add(@Valid @RequestBody RoleRequest request) {
        roleService.add(request);
        return Result.success();
    }
    
    /**
     * 修改角色
     * 权限：role:edit
     */
    @PutMapping
    @PreAuthorize("hasAuthority('role:edit')")
    public Result<Void> update(@Valid @RequestBody RoleUpdateRequest request) {
        roleService.update(request);
        return Result.success();
    }
    
    /**
     * 删除角色
     * 权限：role:delete
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('role:delete')")
    public Result<Void> delete(@PathVariable Long id) {
        roleService.delete(id);
        return Result.success();
    }
    
    /**
     * 给角色分配权限
     * 权限：role:edit
     */
    @PostMapping("/permissions")
    @PreAuthorize("hasAuthority('role:edit')")
    public Result<Void> assignPermissions(@Valid @RequestBody AssignPermissionsRequest request) {
        roleService.assignPermissions(request);
        return Result.success();
    }
    
    /**
     * 获取权限树（用于前端展示）
     * 权限：role:list
     */
    @GetMapping("/permission-tree")
    @PreAuthorize("hasAuthority('role:list')")
    public Result<List<PermissionTreeVO>> getPermissionTree() {
        List<PermissionTreeVO> tree = roleService.getPermissionTree();
        return Result.success(tree);
    }
}
