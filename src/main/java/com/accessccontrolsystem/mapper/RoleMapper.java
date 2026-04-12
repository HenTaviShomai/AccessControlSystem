package com.accessccontrolsystem.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.accessccontrolsystem.entity.Role;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Param;
import java.util.List;

@Mapper
public interface RoleMapper extends BaseMapper<Role> {

    @Select("SELECT r.role_code FROM role r " +
            "LEFT JOIN user_role ur ON r.id = ur.role_id " +
            "WHERE ur.user_id = #{userId} AND r.deleted = 0")
    List<String> selectRoleCodesByUserId(@Param("userId") Long userId);
    @Select("SELECT DISTINCT p.permission_code FROM permission p " +
            "LEFT JOIN role_permission rp ON p.id = rp.permission_id " +
            "LEFT JOIN role r ON rp.role_id = r.id " +
            "LEFT JOIN user_role ur ON r.id = ur.role_id " +
            "WHERE ur.user_id = #{userId} AND r.deleted = 0 AND p.deleted = 0")
    List<String> selectPermissionCodesByUserId(@Param("userId") Long userId);
}