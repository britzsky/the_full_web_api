package com.thefullweb.api.controller;

import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

// 서버 상태 확인용 헬스체크 컨트롤러
@RestController
@RequestMapping("/api/health")
public class HealthController {

    // API 정상 기동 여부 응답
    @GetMapping
    public ResponseEntity<Map<String, Object>> health() {
        return ResponseEntity.ok(Map.of("status", "ok", "service", "the_full_web_api"));
    }
}

