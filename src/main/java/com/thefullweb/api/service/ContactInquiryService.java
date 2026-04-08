package com.thefullweb.api.service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.thefullweb.api.domain.contact.ContactInquiry;
import com.thefullweb.api.domain.contact.ContactReply;
import com.thefullweb.api.dto.contact.ContactInquiryCreateRequest;
import com.thefullweb.api.dto.contact.ContactReplyUpsertRequest;
import com.thefullweb.api.mapper.ContactInquiryMapper;

// 고객문의/답변 도메인 비즈니스 서비스
@Service
public class ContactInquiryService {

    // MyBatis 문의 매퍼 주입
    private final ContactInquiryMapper contactInquiryMapper;

    public ContactInquiryService(ContactInquiryMapper contactInquiryMapper) {
        this.contactInquiryMapper = contactInquiryMapper;
    }

    // 입력 문자열 트리밍
    private String normalize(String value) {
        return value == null ? "" : value.trim();
    }

    // 공백 문자열을 null로 변환
    private String toNullable(String value) {
        String normalized = normalize(value);
        return normalized.isEmpty() ? null : normalized;
    }

    // 문의관리 목록 조회
    public List<ContactInquiry> getInquiryList() {
        return contactInquiryMapper.selectInquiryList();
    }

    // 문의 상세 조회
    public ContactInquiry getInquiry(Long id) {
        return contactInquiryMapper.selectInquiryById(id);
    }

    // 답변 조회
    public ContactReply getReply(Long inquiryId) {
        return contactInquiryMapper.selectReplyByInquiryId(inquiryId);
    }

    // 문의 담당 user_id 반영
    @Transactional
    public ContactInquiry assignInquiryUser(Long inquiryId, String userId) {
        String normalizedUserId = normalize(userId);
        if (inquiryId == null || inquiryId.longValue() <= 0 || normalizedUserId.isEmpty()) {
            return inquiryId == null ? null : contactInquiryMapper.selectInquiryById(inquiryId);
        }

        contactInquiryMapper.updateInquiryAssignedUser(inquiryId, normalizedUserId);
        return contactInquiryMapper.selectInquiryById(inquiryId);
    }

    // 답변 메일 발송 완료 후 문의 답변여부를 완료 상태로 반영
    @Transactional
    public ContactInquiry markInquiryAnswered(Long inquiryId, String userId) {
        String actor = normalize(userId).isEmpty() ? "admin" : normalize(userId);
        ContactInquiry inquiry = contactInquiryMapper.selectInquiryById(inquiryId);
        if (inquiry == null) {
            return null;
        }

        contactInquiryMapper.markInquiryAnswered(inquiryId, actor);
        return contactInquiryMapper.selectInquiryById(inquiryId);
    }

    // 고객문의 등록
    @Transactional
    public ContactInquiry createInquiry(ContactInquiryCreateRequest request) {
        ContactInquiry inquiry = new ContactInquiry();
        inquiry.setBusinessName(normalize(request.getBusinessName()));
        inquiry.setManagerName(normalize(request.getManagerName()));
        inquiry.setPhoneNumber(normalize(request.getPhoneNumber()));
        inquiry.setEmail(normalize(request.getEmail()));
        inquiry.setCurrentMealPrice(normalize(request.getCurrentMealPrice()));
        inquiry.setDesiredMealPrice(normalize(request.getDesiredMealPrice()));
        inquiry.setDailyMealCount(normalize(request.getDailyMealCount()));
        inquiry.setMealType(normalize(request.getMealType()));
        inquiry.setBusinessType(normalize(request.getBusinessType()));
        inquiry.setSwitchingReason(toNullable(request.getSwitchingReason()));
        inquiry.setTitle(normalize(request.getTitle()));
        inquiry.setInquiryContent(normalize(request.getInquiryContent()));
        inquiry.setAnswerYn("N");
        inquiry.setSubmittedAt(resolveSubmittedAt(request.getSubmittedAt()));
        inquiry.setSource(normalize(request.getSource()).isEmpty() ? "contact-page" : normalize(request.getSource()));
        inquiry.setErpSyncTarget(
                normalize(request.getErpSyncTarget()).isEmpty() ? "ERP_INQUIRY_V1" : normalize(request.getErpSyncTarget()));

        contactInquiryMapper.insertInquiry(inquiry);
        return contactInquiryMapper.selectInquiryById(inquiry.getId());
    }

    // 문의 답변 저장/수정
    @Transactional
    public ContactReply upsertReply(Long inquiryId, ContactReplyUpsertRequest request) {
        ContactInquiry inquiry = contactInquiryMapper.selectInquiryById(inquiryId);
        if (inquiry == null) {
            return null;
        }

        String content = normalize(request.getContent());
        String userId = normalize(request.getUserId()).isEmpty() ? "admin" : normalize(request.getUserId());

        ContactReply existing = contactInquiryMapper.selectReplyByInquiryId(inquiryId);
        if (existing == null) {
            ContactReply reply = new ContactReply();
            reply.setInquiryId(inquiryId);
            reply.setContent(content);
            reply.setUserId(userId);
            contactInquiryMapper.insertReply(reply);
        } else {
            existing.setContent(content);
            existing.setModId(userId);
            contactInquiryMapper.updateReply(existing);
        }
        return contactInquiryMapper.selectReplyByInquiryId(inquiryId);
    }

    // 문의 소프트삭제(del_yn='Y')
    @Transactional
    public boolean softDeleteInquiry(Long inquiryId, String deletedBy) {
        String actor = normalize(deletedBy).isEmpty() ? "admin" : normalize(deletedBy);
        int affected = contactInquiryMapper.softDeleteInquiry(inquiryId, actor);
        if (affected <= 0) {
            return false;
        }

        contactInquiryMapper.softDeleteRepliesByInquiryId(inquiryId, actor);
        return true;
    }

    // 제출일 문자열 미지정 시 현재시각 생성
    private String resolveSubmittedAt(String submittedAt) {
        String normalized = normalize(submittedAt);
        if (!normalized.isEmpty()) {
            return normalized;
        }
        return LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
    }
}

