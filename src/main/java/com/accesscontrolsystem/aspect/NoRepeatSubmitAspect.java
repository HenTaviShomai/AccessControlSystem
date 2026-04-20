package com.accesscontrolsystem.aspect;

import com.accesscontrolsystem.annotation.NoRepeatSubmit;
import com.accesscontrolsystem.constant.RedisConstants;
import com.accesscontrolsystem.enums.ErrorCode;
import com.accesscontrolsystem.exception.BusinessException;
import com.accesscontrolsystem.utils.SensitiveDataUtils;
import com.accesscontrolsystem.utils.WebUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.util.concurrent.TimeUnit;

@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class NoRepeatSubmitAspect {
    
    private final RedisTemplate<String, String> redisTemplate;
    
    @Around("@annotation(com.AccessControlSystem.annotation.NoRepeatSubmit)")
    public Object handleNoRepeatSubmit(ProceedingJoinPoint joinPoint) throws Throwable {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        NoRepeatSubmit noRepeatSubmit = method.getAnnotation(NoRepeatSubmit.class);
        
        if (noRepeatSubmit == null) {
            return joinPoint.proceed();
        }
        
        // 构建防重key：用户ID + 类名 + 方法名 + 参数MD5
        String userId = getCurrentUserId();
        String className = method.getDeclaringClass().getSimpleName();
        String methodName = method.getName();
        String paramsHash = getParamsHash(joinPoint.getArgs());
        
        String repeatKey = String.format("%s%s:%s:%s:%s:%s",
                RedisConstants.REPEAT_SUBMIT_KEY,
                userId, className, methodName, paramsHash,
                noRepeatSubmit.duration());
        
        // 尝试设置锁
        Boolean success = redisTemplate.opsForValue()
                .setIfAbsent(repeatKey, "1", noRepeatSubmit.duration(), noRepeatSubmit.timeUnit());
        
        if (Boolean.FALSE.equals(success)) {
            log.warn("重复提交拦截: userId={}, method={}.{}", userId, className, methodName);
            throw new BusinessException(ErrorCode.REPEAT_SUBMIT);
        }
        
        return joinPoint.proceed();
    }
    
    /**
     * 获取当前用户ID
     */
    private String getCurrentUserId() {
        try {
            org.springframework.security.core.Authentication authentication =
                    org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
            if (authentication != null && authentication.isAuthenticated()) {
                Object credentials = authentication.getCredentials();
                if (credentials instanceof Long) {
                    return String.valueOf(credentials);
                }
            }
        } catch (Exception e) {
            log.warn("获取用户ID失败: {}", e.getMessage());
        }
        return "anonymous";
    }
    
    /**
     * 计算参数的Hash值（用于区分不同参数的请求）
     */
    private String getParamsHash(Object[] args) {
        if (args == null || args.length == 0) {
            return "no_params";
        }
        
        // 只取第一个参数（通常是请求体）
        Object param = args[0];
        if (param == null) {
            return "null_param";
        }
        
        // 脱敏后计算hash
        String paramStr = SensitiveDataUtils.desensitize(param);
        return String.valueOf(paramStr.hashCode());
    }
}
