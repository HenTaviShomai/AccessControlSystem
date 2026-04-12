package com.AccesscControlSystem.service.impl;
import com.AccesscControlSystem.constant.RedisConstants;
import com.AccesscControlSystem.service.PermissionService;
import com.AccesscControlSystem.dto.LoginRequest;
import com.AccesscControlSystem.dto.LoginResponse;
import com.AccesscControlSystem.entity.User;
import com.AccesscControlSystem.enums.ErrorCode;
import com.AccesscControlSystem.exception.BusinessException;
import com.AccesscControlSystem.mapper.UserMapper;
import com.AccesscControlSystem.service.AuthService;
import com.AccesscControlSystem.utils.JwtUtils;
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
    private static final String USER_PERMISSION_KEY = "auth:user:permissions:";
    private static final long CACHE_EXPIRE_HOURS = 24;
// 新增

    @Override
    public LoginResponse login(LoginRequest request) {
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
        Long userId = jwtUtils.getUserIdFromToken(token);
        Long remainingTime = jwtUtils.getRemainingTime(token);
        String cacheKey = USER_PERMISSION_KEY + userId;
        redisTemplate.delete(cacheKey);
        if (remainingTime > 0) {
            String blacklistKey = RedisConstants.TOKEN_BLACKLIST_KEY + token;
            redisTemplate.opsForValue().set(blacklistKey, "1", remainingTime, TimeUnit.MILLISECONDS);
        }
        log.info("用户登出成功");
    }
}