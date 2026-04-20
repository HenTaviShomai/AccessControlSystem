package com.accesscontrolsystem.aspect;

import com.accesscontrolsystem.annotation.RateLimit;
import com.accesscontrolsystem.constant.RedisConstants;
import com.accesscontrolsystem.enums.ErrorCode;
import com.accesscontrolsystem.exception.BusinessException;
import com.accesscontrolsystem.utils.WebUtils;
import com.google.common.util.concurrent.RateLimiter;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

@Slf4j
@Aspect
@Component
public class RateLimitAspect {
    
    @Autowired
    private RedisTemplate<String, String> redisTemplate;
    
    // 本地缓存，存储每个key对应的RateLimiter
    private final ConcurrentHashMap<String, RateLimiter> rateLimiterMap = new ConcurrentHashMap<>();
    
    @Before("@annotation(com.AccessControlSystem.annotation.RateLimit)")
    public void checkRateLimit(JoinPoint joinPoint) {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        RateLimit rateLimit = method.getAnnotation(RateLimit.class);
        
        if (rateLimit == null) {
            return;
        }
        
        // 构建限流key：接口名 + 自定义key + 客户端IP
        String clientIp = WebUtils.getClientIp();
        String limitKey = method.getDeclaringClass().getSimpleName() + ":" 
                        + method.getName() + ":" 
                        + rateLimit.key() + ":" 
                        + clientIp;
        
        // 获取或创建RateLimiter
        RateLimiter rateLimiter = rateLimiterMap.computeIfAbsent(
            limitKey,
            k -> RateLimiter.create(rateLimit.permitsPerSecond())
        );
        
        // 尝试获取令牌
        boolean acquired = rateLimiter.tryAcquire(rateLimit.timeout(), rateLimit.timeUnit());
        
        if (!acquired) {
            log.warn("接口限流: ip={}, method={}.{}", clientIp, 
                     method.getDeclaringClass().getSimpleName(), method.getName());
            throw new BusinessException(ErrorCode.REQUEST_TOO_FREQUENT);
        }
    }
}
