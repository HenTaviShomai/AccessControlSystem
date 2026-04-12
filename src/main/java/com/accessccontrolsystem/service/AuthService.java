package com.accessccontrolsystem.service;


import com.accessccontrolsystem.dto.LoginRequest;
import com.accessccontrolsystem.dto.LoginResponse;

public interface AuthService {
    LoginResponse login(LoginRequest request);
    void logout(String token);
}