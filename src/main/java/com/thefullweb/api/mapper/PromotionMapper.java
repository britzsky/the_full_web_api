package com.thefullweb.api.mapper;

import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import com.thefullweb.api.domain.promotion.PromotionPost;

// 홍보 게시글 SQL 매퍼 인터페이스
@Mapper
public interface PromotionMapper {

    // 홍보 게시글 목록 조회(검색 + 삭제 제외)
    List<PromotionPost> selectPostList(@Param("query") String query, @Param("field") String field);

    // 홍보 게시글 상세 조회(삭제 제외)
    PromotionPost selectPostById(@Param("id") Long id);

    // 이전글(현재 id보다 큰 값 중 가장 작은 글) 조회
    PromotionPost selectPreviousPost(@Param("id") Long id);

    // 다음글(현재 id보다 작은 값 중 가장 큰 글) 조회
    PromotionPost selectNextPost(@Param("id") Long id);

    // 홍보 게시글 신규 등록
    int insertPost(PromotionPost post);

    // 홍보 게시글 수정
    int updatePost(PromotionPost post);

    // 홍보 게시글 소프트삭제
    int softDeletePost(@Param("id") Long id);
}

