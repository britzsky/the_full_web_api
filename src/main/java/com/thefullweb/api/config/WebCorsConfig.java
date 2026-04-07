package com.thefullweb.api.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

// 웹 클라이언트(the_full_web) 접근을 위한 CORS 설정
@Configuration
public class WebCorsConfig implements WebMvcConfigurer {

        // API 전역 CORS 정책 적용
        @Override
        public void addCorsMappings(CorsRegistry registry) {
                registry.addMapping("/**")
                                .allowedOrigins(
                                                "http://localhost:8081",
                                                "http://127.0.0.1:8081",
                                                "http://localhost:8090",
                                                "http://127.0.0.1:8090",
                                                "http://52.64.151.137",
                                                "http://52.64.151.137:8081",
                                                "http://52.64.151.137:8090",
                                                "http://n.thefull.kr",
                                                "http://n.thefull.kr:8081",
                                                "http://n.thefull.kr:8090")
                                .allowedMethods("GET", "POST", "PATCH", "DELETE", "OPTIONS")
                                .allowedHeaders("*")
                                .allowCredentials(true)
                                .maxAge(3600);
        }
}
