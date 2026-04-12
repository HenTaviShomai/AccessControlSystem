package com.AccesscControlSystem.service.impl;

import com.AccesscControlSystem.dto.UserPageRequest;
import com.AccesscControlSystem.dto.UserRequest;
import com.AccesscControlSystem.dto.UserResponse;
import com.AccesscControlSystem.dto.UserUpdateRequest;
import com.AccesscControlSystem.entity.User;
import com.AccesscControlSystem.enums.ErrorCode;
import com.AccesscControlSystem.exception.BusinessException;
import com.AccesscControlSystem.mapper.RoleMapper;
import com.AccesscControlSystem.mapper.UserMapper;
import com.AccesscControlSystem.service.UserService;
import com.AccesscControlSystem.vo.PageResult;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserMapper userMapper;
    private final RoleMapper roleMapper;
    private final PasswordEncoder passwordEncoder;

    @Override
    public PageResult<UserResponse> pageList(UserPageRequest request) {
        // 1. 构建分页条件
        Page<User> page = new Page<>(request.getPageNum(), request.getPageSize());
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();

        // 模糊查询
        if (StringUtils.hasText(request.getUsername())) {
            wrapper.like(User::getUsername, request.getUsername());
        }
        // 状态筛选
        if (request.getStatus() != null) {
            wrapper.eq(User::getStatus, request.getStatus());
        }
        // 按创建时间倒序
        wrapper.orderByDesc(User::getCreateTime);

        // 2. 执行分页查询
        IPage<User> userPage = userMapper.selectPage(page, wrapper);

        // 3. 转换为UserResponse（补充角色信息）
        List<UserResponse> list = userPage.getRecords().stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());

        return new PageResult<>(userPage.getTotal(), list);
    }

    @Override
    public UserResponse getById(Long id) {
        User user = userMapper.selectById(id);
        if (user == null) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND);
        }
        return convertToResponse(user);
    }

    @Override
    @Transactional
    public void add(UserRequest request) {
        // 1. 检查用户名是否已存在
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(User::getUsername, request.getUsername());
        if (userMapper.selectCount(wrapper) > 0) {
            throw new BusinessException(ErrorCode.USERNAME_EXISTS);
        }

        // 2. 创建用户
        User user = new User();
        user.setUsername(request.getUsername());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setNickname(request.getNickname());
        user.setStatus(request.getStatus());

        userMapper.insert(user);
        log.info("新增用户成功: username={}", user.getUsername());
    }

    @Override
    @Transactional
    public void update(UserUpdateRequest request) {
        // 1. 检查用户是否存在
        User user = userMapper.selectById(request.getId());
        if (user == null) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND);
        }

        // 2. 更新字段
        if (StringUtils.hasText(request.getNickname())) {
            user.setNickname(request.getNickname());
        }
        if (request.getStatus() != null) {
            user.setStatus(request.getStatus());
        }
        if (StringUtils.hasText(request.getPassword())) {
            user.setPassword(passwordEncoder.encode(request.getPassword()));
        }

        userMapper.updateById(user);
        log.info("更新用户成功: userId={}", user.getId());
    }

    @Override
    @Transactional
    public void delete(Long id) {
        // 逻辑删除（BaseEntity中配置了@TableLogic）
        int result = userMapper.deleteById(id);
        if (result == 0) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND);
        }
        log.info("删除用户成功: userId={}", id);
    }

    @Override
    @Transactional
    public void updateStatus(Long id, Integer status) {
        User user = userMapper.selectById(id);
        if (user == null) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND);
        }
        user.setStatus(status);
        userMapper.updateById(user);
        log.info("更新用户状态成功: userId={}, status={}", id, status);
    }

    /**
     * User实体 → UserResponse
     */
    private UserResponse convertToResponse(User user) {
        UserResponse response = new UserResponse();
        response.setId(user.getId());
        response.setUsername(user.getUsername());
        response.setNickname(user.getNickname());
        response.setStatus(user.getStatus());
        response.setCreateTime(user.getCreateTime());

        // 查询用户的角色
        List<String> roles = roleMapper.selectRoleCodesByUserId(user.getId());
        response.setRoles(roles);

        return response;
    }
}