package com.accesscontrolsystem.service;


import com.accesscontrolsystem.dto.LoginRequest;
import com.accesscontrolsystem.dto.LoginResponse;

public interface AuthService {
    LoginResponse login(LoginRequest request);
    void logout(String token);
}