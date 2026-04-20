package com.accesscontrolsystem.utils;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.Objects;

@Slf4j
public class WebUtils {
    
    /**
     * 获取当前请求的HttpServletRequest
     */
    public static HttpServletRequest getCurrentRequest() {
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        return Objects.requireNonNull(attributes).getRequest();
    }
    
    /**
     * 获取客户端真实IP
     * 考虑反向代理、负载均衡的情况
     */
    public static String getClientIp() {
        HttpServletRequest request = getCurrentRequest();
        
        // 1. 检查 X-Forwarded-For（最常见）
        String ip = request.getHeader("X-Forwarded-For");
        if (ip != null && !ip.isEmpty() && !"unknown".equalsIgnoreCase(ip)) {
            // 多级代理的情况，第一个IP是真实客户端IP
            int index = ip.indexOf(",");
            if (index != -1) {
                return ip.substring(0, index);
            }
            return ip;
        }
        
        // 2. 检查 Proxy-Client-IP（Apache）
        ip = request.getHeader("Proxy-Client-IP");
        if (ip != null && !ip.isEmpty() && !"unknown".equalsIgnoreCase(ip)) {
            return ip;
        }
        
        // 3. 检查 WL-Proxy-Client-IP（WebLogic）
        ip = request.getHeader("WL-Proxy-Client-IP");
        if (ip != null && !ip.isEmpty() && !"unknown".equalsIgnoreCase(ip)) {
            return ip;
        }
        
        // 4. 默认取 remoteAddr
        return request.getRemoteAddr();
    }
}
