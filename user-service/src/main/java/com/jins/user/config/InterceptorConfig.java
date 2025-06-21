package com.jins.user.config;

import com.jins.user.interceptor.JwtInterceptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * 拦截器配置
 * 权限开关
 */
@Configuration
public class InterceptorConfig implements WebMvcConfigurer {

    @Autowired
    private JwtInterceptor jwtInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        //jwt拦截器
        registry.addInterceptor(jwtInterceptor)
                .addPathPatterns("/**")
                .excludePathPatterns(
                        "/user/login",
                        "/user/register",
                        "/error", // 添加系统错误路径
                        "/doc.html",
                        "/webjars/**",
                        "/v2/api-docs/**",
                        "/swagger-resources/**",
                        "/swagger-ui/**",
                        "/favicon.ico"
                )
                .order(0);

        WebMvcConfigurer.super.addInterceptors(registry);
    }


}
