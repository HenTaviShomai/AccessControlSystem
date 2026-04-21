package com.accesscontrolsystem.utils;

import com.accesscontrolsystem.BaseTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("JWT工具类测试")
class JwtUtilsTest extends BaseTest {
    
    @Autowired
    private JwtUtils jwtUtils;
    
    private Long testUserId;
    private String testUsername;
    private String token;
    
    @BeforeEach
    void setUp() {
        testUserId = 1L;
        testUsername = "admin";
        token = jwtUtils.generateToken(testUserId, testUsername);
    }
    
    @Test
    @DisplayName("生成Token - 成功")
    void testGenerateToken() {
        assertNotNull(token);
        assertTrue(token.length() > 0);
    }
    
    @Test
    @DisplayName("从Token获取用户ID - 成功")
    void testGetUserIdFromToken() {
        Long userId = jwtUtils.getUserIdFromToken(token);
        assertEquals(testUserId, userId);
    }
    
    @Test
    @DisplayName("从Token获取用户名 - 成功")
    void testGetUsernameFromToken() {
        String username = jwtUtils.getUsernameFromToken(token);
        assertEquals(testUsername, username);
    }
    
    @Test
    @DisplayName("验证Token - 有效Token返回true")
    void testValidateTokenValid() {
        assertTrue(jwtUtils.validateToken(token));
    }
    
    @Test
    @DisplayName("验证Token - 无效Token返回false")
    void testValidateTokenInvalid() {
        boolean result = jwtUtils.validateToken("invalid.token.here");
        assertFalse(result);
    }
    
    @Test
    @DisplayName("获取Token剩余时间 - 应该大于0")
    void testGetRemainingTime() {
        Long remainingTime = jwtUtils.getRemainingTime(token);
        assertTrue(remainingTime > 0);
        assertTrue(remainingTime <= 1800000);
    }
}
