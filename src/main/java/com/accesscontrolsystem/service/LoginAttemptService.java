package com.accesscontrolsystem.service;

import com.accesscontrolsystem.constant.RedisConstants;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class LoginAttemptService {
    
    private final RedisTemplate<String, String> redisTemplate;
    
    // 最大失败次数
    private static final int MAX_ATTEMPTS = 5;
    
    // 锁定时间（分钟）
    private static final int LOCK_TIME_MINUTES = 15;
    
    /**
     * 登录失败
     */
    public void loginFailed(String username) {
        String failKey = RedisConstants.LOGIN_FAIL_KEY + username;
        String attemptsStr = redisTemplate.opsForValue().get(failKey);
        int attempts = attemptsStr == null ? 0 : Integer.parseInt(attemptsStr);
        
        attempts++;
        
        if (attempts >= MAX_ATTEMPTS) {
            // 达到最大失败次数，锁定账号
            String lockKey = RedisConstants.LOGIN_LOCK_KEY + username;
            redisTemplate.opsForValue().set(lockKey, "1", LOCK_TIME_MINUTES, TimeUnit.MINUTES);
            redisTemplate.delete(failKey);
            log.warn("用户登录失败次数过多已锁定: username={}, 锁定时间={}分钟", username, LOCK_TIME_MINUTES);
        } else {
            redisTemplate.opsForValue().set(failKey, String.valueOf(attempts), 1, TimeUnit.HOURS);
            log.warn("用户登录失败: username={}, 失败次数={}", username, attempts);
        }
    }
    
    /**
     * 登录成功，清除失败记录
     */
    public void loginSucceeded(String username) {
        String failKey = RedisConstants.LOGIN_FAIL_KEY + username;
        redisTemplate.delete(failKey);
        log.debug("用户登录成功，清除失败记录: username={}", username);
    }
    
    /**
     * 检查账号是否被锁定
     */
    public boolean isLocked(String username) {
        String lockKey = RedisConstants.LOGIN_LOCK_KEY + username;
        return Boolean.TRUE.equals(redisTemplate.hasKey(lockKey));
    }
    
    /**
     * 获取剩余锁定时间（秒）
     */
    public Long getRemainingLockTime(String username) {
        String lockKey = RedisConstants.LOGIN_LOCK_KEY + username;
        return redisTemplate.getExpire(lockKey, TimeUnit.SECONDS);
    }
}
