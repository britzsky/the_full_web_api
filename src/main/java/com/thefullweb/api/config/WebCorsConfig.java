package com.thefullweb.api.config;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Set;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

// 웹 클라이언트(the_full_web) 접근을 위한 CORS 설정
@Configuration
public class WebCorsConfig implements WebMvcConfigurer {

        // 허용 오리진 목록(콤마 구분)
        @Value("${app.cors.allowed-origins:http://localhost:8081,http://127.0.0.1:8081,http://52.64.151.137:8081,http://n.thefull.kr,http://n.thefull.kr:8081,http://localhost:8090,http://127.0.0.1:8090,http://52.64.151.137:8090,http://n.thefull.kr,http://n.thefull.kr:8090}")
        private String allowedOrigins;

        // API 전역 CORS 정책 적용
        @Override
        public void addCorsMappings(CorsRegistry registry) {
                String[] origins = allowedOrigins.split(",");
                registry.addMapping("/**")
                                .allowedOrigins(origins)
                                .allowedMethods("GET", "POST", "PATCH", "DELETE", "OPTIONS")
                                .allowedHeaders("*")
                                .allowCredentials(true)
                                .maxAge(3600);
        }
}
