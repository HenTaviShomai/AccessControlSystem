package com.accessccontrolsystem.controller;

import com.accessccontrolsystem.dto.LoginRequest;
import com.accessccontrolsystem.dto.LoginResponse;
import com.accessccontrolsystem.service.AuthService;
import com.accessccontrolsystem.vo.Result;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/login")
    public Result<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        LoginResponse response = authService.login(request);
        return Result.success(response);
    }

    @PostMapping("/logout")
    public Result<Void> logout(@RequestHeader("Authorization") String authorization) {
        String token = authorization.substring(7);
        authService.logout(token);
        return Result.success();
    }
}