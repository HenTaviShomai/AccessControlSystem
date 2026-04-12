package com.accesscontrolsystem.security;

import com.accesscontrolsystem.constant.RedisConstants;
import com.accesscontrolsystem.mapper.PermissionMapper;
import com.accesscontrolsystem.mapper.RoleMapper;
import com.accesscontrolsystem.service.PermissionService;
import com.accesscontrolsystem.utils.JwtUtils;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final RoleMapper roleMapper;
    private final JwtUtils jwtUtils;
    private final PermissionMapper permissionMapper;
    private final RedisTemplate<String, String> redisTemplate;
    private final PermissionService permissionService;
    private static final String USER_PERMISSION_KEY = "auth:user:permissions:";
    public JwtAuthenticationFilter(RoleMapper roleMapper, JwtUtils jwtUtils, PermissionMapper permissionMapper, RedisTemplate<String, String> redisTemplate, PermissionService permissionService) {
        this.roleMapper = roleMapper;
        this.jwtUtils = jwtUtils;
        this.permissionMapper = permissionMapper;
        this.redisTemplate = redisTemplate;
        this.permissionService = permissionService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        String token = getTokenFromRequest(request);

        if (StringUtils.hasText(token) && jwtUtils.validateToken(token)) {

            String blacklistKey = RedisConstants.TOKEN_BLACKLIST_KEY + token;
            Boolean isBlacklisted = redisTemplate.hasKey(blacklistKey);
            if (Boolean.TRUE.equals(isBlacklisted)) {
                log.debug("Token已在黑名单中: token={}", token);
                filterChain.doFilter(request, response);
                return;
            }


            Long userId = jwtUtils.getUserIdFromToken(token);
            String username = jwtUtils.getUsernameFromToken(token);
            // 【关键修改】查询用户的权限列表
            List<String> permissionCodes = roleMapper.selectPermissionCodesByUserId(userId);
            log.debug("用户 {} 的权限: {}", username, permissionCodes);

            // 转换为Spring Security需要的权限格式
            List<SimpleGrantedAuthority> authorities = permissionCodes.stream()
                    .map(SimpleGrantedAuthority::new)
                    .collect(Collectors.toList());

            // 创建认证信息（带上权限）
            UserDetails userDetails = User.builder()
                    .username(username)
                    .password("")
                    .authorities(authorities)
                    .build();

            var authentication =
                    new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
            authentication.setDetails(userId);

            SecurityContextHolder.getContext().setAuthentication(authentication);

            log.debug("用户认证成功: userId={}, username={}, 权限数量={}", userId, username, authorities.size());
        } else if (StringUtils.hasText(token)) {
            log.debug("Token无效或已过期: {}", token);
        }

        filterChain.doFilter(request, response);
    }
    /**
     * 从Redis获取用户权限
     */
    /**
     * 从Redis获取用户权限（修复类型转换问题）
     */
    @SuppressWarnings("unchecked")
    private Set<String> getUserPermissions(Long userId) {
        String cacheKey = USER_PERMISSION_KEY + userId;

        try {
            Object value = redisTemplate.opsForValue().get(cacheKey);

            if (value == null) {
                log.warn("用户权限缓存不存在: userId={}", userId);
                return new HashSet<>();
            }

            // 处理不同的存储类型
            if (value instanceof Set) {
                return (Set<String>) value;
            } else if (value instanceof String) {
                // 如果是字符串，尝试解析
                String str = (String) value;
                Set<String> result = new HashSet<>();
                if (str.startsWith("[") && str.endsWith("]")) {
                    String content = str.substring(1, str.length() - 1);
                    if (!content.isEmpty()) {
                        for (String s : content.split(",")) {
                            result.add(s.trim());
                        }
                    }
                }
                return result;
            } else {
                log.warn("用户权限缓存格式异常: userId={}, type={}", userId, value.getClass());
                return new HashSet<>();
            }
        } catch (Exception e) {
            log.error("获取用户权限失败: userId={}", userId, e);
            return new HashSet<>();
        }
    }

    private String getTokenFromRequest(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        return null;
    }
}