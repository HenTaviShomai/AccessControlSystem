package com.accesscontrolsystem.constant;

public class RedisConstants {
    
    /**
     * 用户权限缓存Key
     * 格式: auth:user:permissions:{userId}
     */
    public static final String USER_PERMISSIONS_KEY = "auth:user:permissions:";
    
    /**
     * Token黑名单Key
     * 格式: auth:token:blacklist:{token}
     */
    public static final String TOKEN_BLACKLIST_KEY = "auth:token:blacklist:";
    
    /**
     * 登录失败次数Key
     * 格式: auth:login:fail:{username}
     */
    public static final String LOGIN_FAIL_KEY = "auth:login:fail:";
    
    /**
     * 权限缓存过期时间（秒），默认30分钟
     */
    public static final long PERMISSIONS_EXPIRE_SECONDS = 1800;
}
