package com.accesscontrolsystem.security;

import com.accesscontrolsystem.constant.RedisConstants;
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
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final RoleMapper roleMapper;
    private final JwtUtils jwtUtils;
    private final RedisTemplate<String, String> redisTemplate;
    private final PermissionService permissionService;
    public JwtAuthenticationFilter(RoleMapper roleMapper, JwtUtils jwtUtils, RedisTemplate<String, String> redisTemplate, PermissionService permissionService) {
        this.roleMapper = roleMapper;
        this.jwtUtils = jwtUtils;
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

    private String getTokenFromRequest(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        return null;
    }
}