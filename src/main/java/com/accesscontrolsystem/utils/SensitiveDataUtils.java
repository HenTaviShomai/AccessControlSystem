package com.AccessControlSystem.utils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

@Slf4j
public class SensitiveDataUtils {
    
    private static final ObjectMapper objectMapper = new ObjectMapper();
    
    // 敏感字段名（不区分大小写）
    private static final Pattern SENSITIVE_FIELDS = Pattern.compile(
        "(?i)(password|pwd|token|secret|authorization|access_token|refresh_token)"
    );
    
    // 手机号正则
    private static final Pattern PHONE_PATTERN = Pattern.compile("1[3-9]\d{9}");
    
    // 身份证正则
    private static final Pattern ID_CARD_PATTERN = Pattern.compile("[1-9]\d{16}[0-9Xx]");
    
    /**
     * 脱敏请求参数
     */
    public static String desensitize(Object params) {
        if (params == null) {
            return "";
        }
        
        try {
            String json = objectMapper.writeValueAsString(params);
            json = desensitizeJson(json);
            
            // 限制长度，防止日志过大
            if (json.length() > 2000) {
                json = json.substring(0, 2000) + "...(truncated)";
            }
            return json;
        } catch (JsonProcessingException e) {
            log.warn("参数序列化失败: {}", e.getMessage());
            return params.toString();
        }
    }
    
    /**
     * 脱敏JSON字符串
     */
    private static String desensitizeJson(String json) {
        // 脱敏敏感字段
        json = SENSITIVE_FIELDS.matcher(json).replaceAll(m -> {
            String match = m.group();
            return match.substring(0, Math.min(3, match.length())) + "***";
        });
        
        // 脱敏手机号
        json = PHONE_PATTERN.matcher(json).replaceAll(m -> {
            String phone = m.group();
            return phone.substring(0, 3) + "****" + phone.substring(7);
        });
        
        // 脱敏身份证
        json = ID_CARD_PATTERN.matcher(json).replaceAll("************");
        
        return json;
    }
}
