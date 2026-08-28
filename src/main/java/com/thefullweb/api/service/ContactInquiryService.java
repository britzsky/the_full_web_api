package com.thefullweb.api.service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
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

    private static final List<String> MEAL_TYPE_OPTIONS = List.of("조식", "중식", "석식", "기타");
    private static final String MEAL_TYPE_OTHER = "기타";

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

    // 답변 메일 발송 완료 후 저장된 답변의 이메일 발송 상태를 반영
    @Transactional
    public ContactReply markReplyEmailSent(Long inquiryId) {
        ContactReply reply = contactInquiryMapper.selectReplyByInquiryId(inquiryId);
        if (reply == null) {
            return null;
        }

        contactInquiryMapper.markReplyEmailSent(inquiryId);
        return contactInquiryMapper.selectReplyByInquiryId(inquiryId);
    }

    // 고객문의 등록
    @Transactional
    public ContactInquiry createInquiry(ContactInquiryCreateRequest request) {
        ContactInquiry inquiry = new ContactInquiry();
        inquiry.setBusinessName(normalize(request.getBusinessName()));
        inquiry.setManagerName(normalize(request.getManagerName()));
        inquiry.setPhoneNumber(normalize(request.getPhoneNumber()));
        inquiry.setEmail(normalize(request.getEmail()));
        // 화면에서 값을 받지 않는 항목(인스타그램 간편문의 등)은 DB에 실제 NULL로 저장한다.
        inquiry.setCurrentMealPrice(toNullable(request.getCurrentMealPrice()));
        inquiry.setDesiredMealPrice(toNullable(request.getDesiredMealPrice()));
        inquiry.setDailyMealCount(toNullable(request.getDailyMealCount()));
        inquiry.setMealType(resolveMealType(request));
        inquiry.setBusinessType(toNullable(request.getBusinessType()));
        inquiry.setSwitchingReason(toNullable(request.getSwitchingReason()));
        inquiry.setTitle(toNullable(request.getTitle()));
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

        // 문의 알림은 이메일 발송이 아니라 답변 저장 완료를 기준으로 종료한다.
        contactInquiryMapper.markInquiryAnswered(inquiryId, userId);
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

    // 복수 선택 식사 구분을 기존 meal_type 컬럼과 관리 화면에서 사용하는 표시 문자열로 조합
    private String resolveMealType(ContactInquiryCreateRequest request) {
        List<String> requestedMealTypes = request.getMealTypes();
        if (requestedMealTypes == null || requestedMealTypes.isEmpty()) {
            return toNullable(request.getMealType());
        }

        Set<String> selectedMealTypes = new LinkedHashSet<>();
        for (String requestedMealType : requestedMealTypes) {
            String normalizedMealType = normalize(requestedMealType);
            if (MEAL_TYPE_OPTIONS.contains(normalizedMealType)) {
                selectedMealTypes.add(normalizedMealType);
            }
        }

        List<String> mealTypeLabels = new ArrayList<>();
        for (String mealTypeOption : MEAL_TYPE_OPTIONS) {
            if (!selectedMealTypes.contains(mealTypeOption)) {
                continue;
            }

            if (MEAL_TYPE_OTHER.equals(mealTypeOption)) {
                mealTypeLabels.add(MEAL_TYPE_OTHER + ": " + normalize(request.getMealTypeOther()));
            } else {
                mealTypeLabels.add(mealTypeOption);
            }
        }

        return toNullable(String.join(", ", mealTypeLabels));
    }
}

