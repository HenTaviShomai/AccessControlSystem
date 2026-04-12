package com.AccessControlSystem.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.AccessControlSystem.entity.UserRole;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Delete;

import java.util.List;

@Mapper
public interface UserRoleMapper extends BaseMapper<UserRole> {
    /**
     * 根据角色ID查询用户ID列表
     */
    @Select("SELECT user_id FROM user_role WHERE role_id = #{roleId}")
    List<Long> selectUserIdsByRoleId(@Param("roleId") Long roleId);
    /**
     * 根据用户ID删除所有角色关联
     */
    @Delete("DELETE FROM user_role WHERE user_id = #{userId}")
    int deleteByUserId(@Param("userId") Long userId);
    
    /**
     * 根据用户ID查询角色ID列表
     */
    @Select("SELECT role_id FROM user_role WHERE user_id = #{userId}")
    List<Long> selectRoleIdsByUserId(@Param("userId") Long userId);
}
