package com.accesscontrolsystem.service.impl;

import com.accesscontrolsystem.BaseTest;
import com.accesscontrolsystem.dto.UserRequest;
import com.accesscontrolsystem.dto.UserResponse;
import com.accesscontrolsystem.entity.User;
import com.accesscontrolsystem.enums.ErrorCode;
import com.accesscontrolsystem.exception.BusinessException;
import com.accesscontrolsystem.mapper.RoleMapper;
import com.accesscontrolsystem.mapper.UserMapper;
import com.accesscontrolsystem.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.jdbc.Sql;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
@SpringBootTest
@Sql(scripts = {"/schema.sql", "/data.sql"}, executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
@DisplayName("用户服务测试")
class UserServiceTest extends BaseTest {
    
    @Autowired
    private UserService userService;
    
    @MockitoBean
    private UserMapper userMapper;
    
    @MockitoBean
    private RoleMapper roleMapper;
    
    @MockitoBean
    private PasswordEncoder passwordEncoder;
    
    private User testUser;
    
    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setId(1L);
        testUser.setUsername("admin");
        testUser.setPassword("$2a$10$encrypted");
        testUser.setNickname("管理员");
        testUser.setStatus(1);
    }
    
    @Test
    @DisplayName("根据ID查询用户 - 成功")
    void testGetByIdSuccess() {
        when(userMapper.selectById(1L)).thenReturn(testUser);
        when(roleMapper.selectRoleCodesByUserId(1L)).thenReturn(List.of("ADMIN"));
        
        UserResponse response = userService.getById(1L);
        
        assertNotNull(response);
        assertEquals(1L, response.getId());
        assertEquals("admin", response.getUsername());
        assertEquals("管理员", response.getNickname());
        assertEquals(1, response.getStatus());
        assertTrue(response.getRoles().contains("ADMIN"));
        
        verify(userMapper, times(1)).selectById(1L);
    }
    
    @Test
    @DisplayName("根据ID查询用户 - 用户不存在抛异常")
    void testGetByIdNotFound() {
        when(userMapper.selectById(999L)).thenReturn(null);
        
        BusinessException exception = assertThrows(BusinessException.class, () -> {
            userService.getById(999L);
        });
        
        assertEquals(ErrorCode.USER_NOT_FOUND.getCode(), exception.getCode());
    }
    
    @Test
    @DisplayName("新增用户 - 成功")
    void testAddSuccess() {
        UserRequest request = new UserRequest();
        request.setUsername("newuser");
        request.setPassword("123456");
        request.setNickname("新用户");
        request.setStatus(1);
        
        when(userMapper.selectCount(any())).thenReturn(0L);
        when(passwordEncoder.encode("123456")).thenReturn("$2a$10$encrypted");
        when(userMapper.insert(any(User.class))).thenReturn(1);
        
        assertDoesNotThrow(() -> userService.add(request));
        
        verify(userMapper, times(1)).insert(any(User.class));
    }
    
    @Test
    @DisplayName("新增用户 - 用户名已存在抛异常")
    void testAddDuplicateUsername() {
        UserRequest request = new UserRequest();
        request.setUsername("admin");
        request.setPassword("123456");
        
        when(userMapper.selectCount(any())).thenReturn(1L);
        
        BusinessException exception = assertThrows(BusinessException.class, () -> {
            userService.add(request);
        });
        
        assertEquals(ErrorCode.USERNAME_EXISTS.getCode(), exception.getCode());
        verify(userMapper, never()).insert(any((User.class)));
    }
    
    @Test
    @DisplayName("删除用户 - 成功")
    void testDeleteSuccess() {
        when(userMapper.deleteById(1L)).thenReturn(1);
        
        assertDoesNotThrow(() -> userService.delete(1L));
        
        verify(userMapper, times(1)).deleteById(1L);
    }
    
    @Test
    @DisplayName("删除用户 - 用户不存在抛异常")
    void testDeleteNotFound() {
        when(userMapper.deleteById(999L)).thenReturn(0);
        
        BusinessException exception = assertThrows(BusinessException.class, () -> {
            userService.delete(999L);
        });
        
        assertEquals(ErrorCode.USER_NOT_FOUND.getCode(), exception.getCode());
    }
    
    @Test
    @DisplayName("更新用户状态 - 成功")
    void testUpdateStatusSuccess() {
        when(userMapper.selectById(1L)).thenReturn(testUser);
        when(userMapper.updateById(any(User.class))).thenReturn(1);
        
        assertDoesNotThrow(() -> userService.updateStatus(1L, 0));
        
        assertEquals(0, testUser.getStatus());
        verify(userMapper, times(1)).updateById(testUser);
    }
}
