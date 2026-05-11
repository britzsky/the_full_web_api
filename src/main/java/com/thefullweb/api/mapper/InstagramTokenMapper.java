package com.thefullweb.api.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import com.thefullweb.api.domain.instagram.InstagramToken;

// 인스타그램 장기 토큰 저장 및 갱신 SQL 매퍼
@Mapper
public interface InstagramTokenMapper {

    // 인스타그램 피드 조회에 사용할 최신 토큰을 조회
    InstagramToken selectCurrentToken();

    // 신규 장기 토큰을 기본 토큰으로 저장
    int insertToken(
            @Param("accessToken") String accessToken,
            @Param("expiresInSeconds") long expiresInSeconds);

    // 기존 장기 토큰을 갱신 결과로 교체
    int updateToken(
            @Param("id") Long id,
            @Param("accessToken") String accessToken,
            @Param("expiresInSeconds") long expiresInSeconds);
}
