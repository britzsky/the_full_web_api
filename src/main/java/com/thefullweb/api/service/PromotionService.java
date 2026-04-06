package com.thefullweb.api.service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.thefullweb.api.domain.promotion.PromotionPost;
import com.thefullweb.api.dto.promotion.PromotionPostCreateRequest;
import com.thefullweb.api.dto.promotion.PromotionPostUpdateRequest;
import com.thefullweb.api.mapper.PromotionMapper;

// 홍보 게시글 도메인 비즈니스 서비스
@Service
public class PromotionService {

    // MyBatis 홍보 매퍼 주입
    private final PromotionMapper promotionMapper;

    public PromotionService(PromotionMapper promotionMapper) {
        this.promotionMapper = promotionMapper;
    }

    // 입력 문자열 트리밍
    private String normalize(String value) {
        return value == null ? "" : value.trim();
    }

    // 홍보 게시글 목록 조회
    public List<PromotionPost> getPostList(String query, String field) {
        String normalizedQuery = normalize(query);
        String normalizedField = normalize(field);

        if (!"title".equals(normalizedField) && !"content".equals(normalizedField) && !"all".equals(normalizedField)) {
            normalizedField = "title";
        }

        return promotionMapper.selectPostList(normalizedQuery, normalizedField);
    }

    // 홍보 게시글 상세 조회
    public PromotionPost getPost(Long id) {
        return promotionMapper.selectPostById(id);
    }

    // 홍보 인접글(이전글/다음글) 조회
    public Map<String, PromotionPost> getAdjacent(Long id) {
        Map<String, PromotionPost> result = new LinkedHashMap<>();
        result.put("previous", promotionMapper.selectPreviousPost(id));
        result.put("next", promotionMapper.selectNextPost(id));
        return result;
    }

    // 홍보 게시글 등록
    @Transactional
    public PromotionPost createPost(PromotionPostCreateRequest request) {
        PromotionPost post = new PromotionPost();
        post.setTitle(normalize(request.getTitle()));
        post.setContent(normalize(request.getContent()));
        post.setAuthor(normalize(request.getAuthor()).isEmpty() ? "더채움" : normalize(request.getAuthor()));
        promotionMapper.insertPost(post);
        return promotionMapper.selectPostById(post.getId());
    }

    // 홍보 게시글 수정
    @Transactional
    public PromotionPost updatePost(Long id, PromotionPostUpdateRequest request) {
        PromotionPost current = promotionMapper.selectPostById(id);
        if (current == null) {
            return null;
        }

        String title = normalize(request.getTitle());
        String content = normalize(request.getContent());
        String author = normalize(request.getAuthor());

        current.setTitle(title.isEmpty() ? current.getTitle() : title);
        current.setContent(content.isEmpty() ? current.getContent() : content);
        current.setAuthor(author.isEmpty() ? current.getAuthor() : author);

        promotionMapper.updatePost(current);
        return promotionMapper.selectPostById(id);
    }

    // 홍보 게시글 소프트삭제(del_yn='Y')
    @Transactional
    public boolean softDeletePost(Long id) {
        return promotionMapper.softDeletePost(id) > 0;
    }
}

