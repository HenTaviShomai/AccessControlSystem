package com.accessccontrolsystem.service;

import com.accessccontrolsystem.dto.UserRequest;
import com.accessccontrolsystem.dto.UserResponse;
import com.accessccontrolsystem.dto.UserUpdateRequest;
import com.accessccontrolsystem.vo.PageResult;
import com.accessccontrolsystem.dto.UserPageRequest;

public interface UserService {

    PageResult<UserResponse> pageList(UserPageRequest request);

    UserResponse getById(Long id);

    void add(UserRequest request);

    void update(UserUpdateRequest request);

    void delete(Long id);

    void updateStatus(Long id, Integer status);
}