package com.accesscontrolsystem.config;


import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        // 1. 定义安全方案名称（如 JWT 认证）
        String securitySchemeName = "BearerAuth";

        return new OpenAPI()
                // 配置文档基本信息
                .info(new Info()
                        .title("统一认证授权中心 - API接口文档")
                        .version("1.0.0")
                        .description("基于 Spring Boot 3.x + Spring Security 6.x 的权限与审计底座系统")
                        .contact(new Contact().name("研发团队").email("team@example.com")))

                // 2. 声明全局安全请求：让所有接口默认都支持该认证方案
                .addSecurityItem(new SecurityRequirement().addList(securitySchemeName))

                // 3. 在组件中定义具体的安全方案格式
                .components(new Components()
                        .addSecuritySchemes(securitySchemeName,
                                new SecurityScheme()
                                        .name("Authorization") // HTTP 请求头的名字
                                        .type(SecurityScheme.Type.HTTP) // 方案类型为 HTTP
                                        .scheme("bearer") // 具体的协议是 bearer token
                                        .bearerFormat("JWT") // 额外说明格式为 JWT
                        ));
    }
}