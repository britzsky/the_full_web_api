package com.thefullweb.api.mapper;

import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import com.thefullweb.api.domain.contact.ContactInquiry;
import com.thefullweb.api.domain.contact.ContactReply;

// 고객문의/답변 테이블 SQL 매퍼 인터페이스
@Mapper
public interface ContactInquiryMapper {

    // 고객문의 신규 등록
    int insertInquiry(ContactInquiry inquiry);

    // 문의관리 목록 조회(삭제 제외)
    List<ContactInquiry> selectInquiryList();

    // 문의 상세 조회(삭제 제외)
    ContactInquiry selectInquiryById(@Param("id") Long id);

    // 문의 답변 조회(최신 1건)
    ContactReply selectReplyByInquiryId(@Param("inquiryId") Long inquiryId);

    // 문의 답변 신규 등록
    int insertReply(ContactReply reply);

    // 문의 답변 수정
    int updateReply(ContactReply reply);

    // 문의 답변여부 Y 업데이트
    int markInquiryAnswered(@Param("id") Long id, @Param("modId") String modId);

    // 문의 소프트삭제
    int softDeleteInquiry(@Param("id") Long id, @Param("modId") String modId);

    // 해당 문의의 답변 소프트삭제
    int softDeleteRepliesByInquiryId(@Param("inquiryId") Long inquiryId, @Param("modId") String modId);
}

