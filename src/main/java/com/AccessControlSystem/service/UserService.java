package com.AccessControlSystem.service;

import com.AccessControlSystem.dto.UserRequest;
import com.AccessControlSystem.dto.UserResponse;
import com.AccessControlSystem.dto.UserUpdateRequest;
import com.AccessControlSystem.vo.PageResult;
import com.AccessControlSystem.dto.UserPageRequest;

public interface UserService {

    PageResult<UserResponse> pageList(UserPageRequest request);

    UserResponse getById(Long id);

    void add(UserRequest request);

    void update(UserUpdateRequest request);

    void delete(Long id);

    void updateStatus(Long id, Integer status);
}