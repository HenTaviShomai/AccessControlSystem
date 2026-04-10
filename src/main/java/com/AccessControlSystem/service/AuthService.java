package com.AccessControlSystem.service;


import com.AccessControlSystem.dto.LoginRequest;
import com.AccessControlSystem.dto.LoginResponse;

public interface AuthService {
    LoginResponse login(LoginRequest request);
    void logout(String token);
}