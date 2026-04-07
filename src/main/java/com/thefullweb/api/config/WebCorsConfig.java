package com.thefullweb.api.config;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Set;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

// 웹 클라이언트(the_full_web) 접근을 위한 전역 CORS 설정
@Configuration
public class WebCorsConfig implements WebMvcConfigurer {

    // 운영 설정 파일에서 추가 허용 origin 목록을 주입
    @Value("${app.cors.allowed-origins:}")
    private String allowedOriginsProperty;

    // API 전역 CORS 정책 적용
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
                .allowedOriginPatterns(resolveAllowedOriginPatterns())
                .allowedMethods("*")
                .allowedHeaders("*")
                .exposedHeaders("*")
                .allowCredentials(true)
                .maxAge(3600);
    }

    // 개발 및 운영 환경에서 자주 사용하는 origin 패턴을 함께 허용
    private String[] resolveAllowedOriginPatterns() {
        Set<String> allowedOriginPatterns = new LinkedHashSet<>(Arrays.asList(
                "http://localhost",
                "https://localhost",
                "http://localhost:*",
                "https://localhost:*",
                "http://127.0.0.1",
                "https://127.0.0.1",
                "http://127.0.0.1:*",
                "https://127.0.0.1:*",
                "http://52.64.151.137",
                "https://52.64.151.137",
                "http://52.64.151.137:*",
                "https://52.64.151.137:*",
                "http://n.thefull.kr",
                "https://n.thefull.kr",
                "http://n.thefull.kr:*",
                "https://n.thefull.kr:*",
                "http://*.thefull.kr",
                "https://*.thefull.kr",
                "http://*.thefull.kr:*",
                "https://*.thefull.kr:*"));

        if (allowedOriginsProperty != null && !allowedOriginsProperty.isBlank()) {
            Arrays.stream(allowedOriginsProperty.split(","))
                    .map(String::trim)
                    .filter(origin -> !origin.isEmpty())
                    .forEach(allowedOriginPatterns::add);
        }

        return allowedOriginPatterns.toArray(String[]::new);
    }
}
