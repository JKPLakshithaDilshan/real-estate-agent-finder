package com.realestate.app.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.lang.NonNull;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class AppConfig implements WebMvcConfigurer {
    private final RoleGuardInterceptor roleGuardInterceptor;

    public AppConfig(RoleGuardInterceptor roleGuardInterceptor) {
        this.roleGuardInterceptor = roleGuardInterceptor;
    }

    @Override
    public void addInterceptors(@NonNull InterceptorRegistry registry) {
        registry.addInterceptor(roleGuardInterceptor)
                .addPathPatterns("/**")
                .excludePathPatterns(
                        "/",
                        "/login",
                        "/register",
                        "/logout",
                        "/admin/login",
                        "/css/**",
                        "/js/**",
                        "/images/**",
                        "/vendor/**",
                        "/error",
                        "/favicon.ico");
    }
}
