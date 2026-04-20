package com.accesscontrolsystem.service.impl;
import com.accesscontrolsystem.constant.RedisConstants;
import com.accesscontrolsystem.service.PermissionService;
import com.accesscontrolsystem.dto.LoginRequest;
import com.accesscontrolsystem.dto.LoginResponse;
import com.accesscontrolsystem.entity.User;
import com.accesscontrolsystem.enums.ErrorCode;
import com.accesscontrolsystem.exception.BusinessException;
import com.accesscontrolsystem.mapper.UserMapper;
import com.accesscontrolsystem.service.AuthService;
import com.accesscontrolsystem.utils.JwtUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtils jwtUtils;
    private final RedisTemplate<String, String> redisTemplate;
    private final PermissionService permissionService;
    private final com.accesscontrolsystem.service.LoginAttemptService loginAttemptService;

    @Override
    public LoginResponse login(LoginRequest request) {
        String username = request.getUsername();
        if (loginAttemptService.isLocked(username)) {
            Long remainingTime = loginAttemptService.getRemainingLockTime(username);
            throw new BusinessException(ErrorCode.USER_LOCKED,
                    String.format("账号已被锁定，请%d分钟后重试", remainingTime / 60));
        }
        // 1. 查询用户
        User user = userMapper.selectByUsername(request.getUsername());
        if (user == null) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND);
        }

        // 2. 校验密码
        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new BusinessException(ErrorCode.PASSWORD_ERROR);
        }

        // 3. 检查用户状态
        if (user.getStatus() == 0) {
            throw new BusinessException(ErrorCode.USER_DISABLED);
        }

        // 4. 生成Token
        String token = jwtUtils.generateToken(user.getId(), user.getUsername());
        String refreshToken = jwtUtils.generateRefreshToken(user.getId());
        Long expireIn = jwtUtils.getRemainingTime(token);
        permissionService.getUserPermissions(user.getId());
        // 5. 用户信息
        LoginResponse.UserInfo userInfo = new LoginResponse.UserInfo(
                user.getId(),
                user.getUsername(),
                user.getNickname()
        );

        log.info("用户登录成功: userId={}, username={}", user.getId(), user.getUsername());
        return new LoginResponse(token, refreshToken, expireIn, userInfo);
    }

    @Override
    public void logout(String token) {
        // 将Token加入黑名单
        Long remainingTime = jwtUtils.getRemainingTime(token);
        if (remainingTime > 0) {
            String blacklistKey = RedisConstants.TOKEN_BLACKLIST_KEY + token;
            redisTemplate.opsForValue().set(blacklistKey, "1", remainingTime, TimeUnit.MILLISECONDS);
        }
        log.info("用户登出成功");
    }
}