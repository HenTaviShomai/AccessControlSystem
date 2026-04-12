package com.accesscontrolsystem.service;

import com.accesscontrolsystem.dto.UserRequest;
import com.accesscontrolsystem.dto.UserResponse;
import com.accesscontrolsystem.dto.UserUpdateRequest;
import com.accesscontrolsystem.vo.PageResult;
import com.accesscontrolsystem.dto.UserPageRequest;

public interface UserService {

    PageResult<UserResponse> pageList(UserPageRequest request);

    UserResponse getById(Long id);

    void add(UserRequest request);

    void update(UserUpdateRequest request);

    void delete(Long id);

    void updateStatus(Long id, Integer status);
}