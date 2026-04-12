package com.AccesscControlSystem.service;


import com.AccesscControlSystem.dto.LoginRequest;
import com.AccesscControlSystem.dto.LoginResponse;

public interface AuthService {
    LoginResponse login(LoginRequest request);
    void logout(String token);
}