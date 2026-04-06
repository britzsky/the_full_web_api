package com.thefullweb.api;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

// the_full_web_api 스프링부트 진입점
@SpringBootApplication
@MapperScan("com.thefullweb.api.mapper")
public class TheFullWebApiApplication {

    // 애플리케이션 실행 메서드
    public static void main(String[] args) {
        SpringApplication.run(TheFullWebApiApplication.class, args);
    }
}

