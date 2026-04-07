package com.thefullweb.api.controller;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import com.thefullweb.api.domain.promotion.PromotionPost;
import com.thefullweb.api.dto.common.MessageResponse;
import com.thefullweb.api.dto.promotion.PromotionPostCreateRequest;
import com.thefullweb.api.dto.promotion.PromotionPostUpdateRequest;
import com.thefullweb.api.service.PromotionService;

import com.thefullweb.api.config.WebCorsConfig;

// 홍보 게시글 API 컨트롤러
@RestController
@RequestMapping("/promotion/posts")
public class PromotionController {

    // 홍보 도메인 서비스 주입
    private final PromotionService promotionService;

    public PromotionController(PromotionService promotionService, WebCorsConfig webCorsConfig) {
        this.promotionService = promotionService;
    }

    // 홍보 게시글 목록 조회(검색 가능)
    @GetMapping
    public ResponseEntity<Map<String, Object>> listPosts(
            @RequestParam(value = "q", required = false) String query,
            @RequestParam(value = "field", required = false) String field) {
        List<PromotionPost> posts = promotionService.getPostList(query, field);
        return ResponseEntity.ok(Map.of("posts", posts));
    }

    // 홍보 게시글 상세 조회
    @GetMapping("/{id}")
    public ResponseEntity<?> getPost(@PathVariable("id") Long id) {
        PromotionPost post = promotionService.getPost(id);
        if (post == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", "게시글을 찾을 수 없습니다."));
        }
        return ResponseEntity.ok(Map.of("post", post));
    }

    // 홍보 게시글 인접글 조회
    @GetMapping("/{id}/adjacent")
    public ResponseEntity<Map<String, Object>> getAdjacent(@PathVariable("id") Long id) {
        Map<String, PromotionPost> adjacent = promotionService.getAdjacent(id);
        return ResponseEntity.ok(Map.of("adjacent", adjacent));
    }

    // 홍보 게시글 등록
    @PostMapping
    public ResponseEntity<?> createPost(@RequestBody PromotionPostCreateRequest request) {
        if (isBlank(request.getTitle()) || isBlank(request.getContent())) {
            return ResponseEntity.badRequest().body(Map.of("error", "제목과 내용을 입력해 주세요."));
        }

        PromotionPost post = promotionService.createPost(request);
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("message", "게시글이 등록되었습니다.");
        payload.put("post", post);
        return ResponseEntity.status(HttpStatus.CREATED).body(payload);
    }

    // 홍보 게시글 수정
    @PatchMapping("/{id}")
    public ResponseEntity<?> updatePost(@PathVariable("id") Long id, @RequestBody PromotionPostUpdateRequest request) {
        PromotionPost updated = promotionService.updatePost(id, request);
        if (updated == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", "게시글을 찾을 수 없습니다."));
        }

        return ResponseEntity.ok(Map.of("message", "게시글이 수정되었습니다.", "post", updated));
    }

    // 홍보 게시글 소프트삭제
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deletePost(@PathVariable("id") Long id) {
        boolean deleted = promotionService.softDeletePost(id);
        if (!deleted) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", "게시글을 찾을 수 없습니다."));
        }
        return ResponseEntity.ok(new MessageResponse("게시글이 삭제되었습니다."));
    }

    // 빈 문자열/공백 문자열 체크
    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}

