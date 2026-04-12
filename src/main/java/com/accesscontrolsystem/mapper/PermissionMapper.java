package com.accesscontrolsystem.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.accesscontrolsystem.entity.Permission;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface PermissionMapper extends BaseMapper<Permission> {
    
    /**
     * 根据角色ID查询权限码列表
     */
    @Select("SELECT p.permission_code FROM permission p " +
            "LEFT JOIN role_permission rp ON p.id = rp.permission_id " +
            "WHERE rp.role_id = #{roleId} AND p.deleted = 0")
    List<String> selectPermissionCodesByRoleId(@Param("roleId") Long roleId);
    
    /**
     * 查询所有权限（树形结构用）
     */
    @Select("SELECT * FROM permission WHERE deleted = 0 ORDER BY sort ASC")
    List<Permission> selectAllPermissions();
}
