package com.AccesscControlSystem.service;

import com.AccesscControlSystem.dto.UserRequest;
import com.AccesscControlSystem.dto.UserResponse;
import com.AccesscControlSystem.dto.UserUpdateRequest;
import com.AccesscControlSystem.vo.PageResult;
import com.AccesscControlSystem.dto.UserPageRequest;

public interface UserService {

    PageResult<UserResponse> pageList(UserPageRequest request);

    UserResponse getById(Long id);

    void add(UserRequest request);

    void update(UserUpdateRequest request);

    void delete(Long id);

    void updateStatus(Long id, Integer status);
}