package com.AccessControlSystem;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

@SpringBootApplication
public class AccessControlSystemApplication {

    public static void main(String[] args) {
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

        // 加密 123456
        String encodedPassword = encoder.encode("123456");
        System.out.println("123456 的加密结果: " + encodedPassword);

        // 验证密码是否匹配
        boolean matches = encoder.matches("123456", encodedPassword);
        System.out.println("验证结果: " + matches);
        SpringApplication.run(AccessControlSystemApplication.class, args);
    }

}
