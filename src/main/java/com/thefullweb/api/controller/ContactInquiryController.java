package com.thefullweb.api.controller;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.thefullweb.api.domain.contact.ContactInquiry;
import com.thefullweb.api.domain.contact.ContactReply;
import com.thefullweb.api.dto.contact.ContactInquiryCreateRequest;
import com.thefullweb.api.dto.contact.ContactReplyUpsertRequest;
import com.thefullweb.api.dto.common.MessageResponse;
import com.thefullweb.api.service.ContactInquiryService;

// 고객문의/문의관리 API 컨트롤러
@RestController
@RequestMapping({ "/contact/inquiry_management", "/api/contact/inquiry" })
public class ContactInquiryController {

    // 문의 도메인 서비스 주입
    private final ContactInquiryService contactInquiryService;

    public ContactInquiryController(ContactInquiryService contactInquiryService) {
        this.contactInquiryService = contactInquiryService;
    }

    // 문의관리 API: 문의 목록 조회
    @GetMapping
    public ResponseEntity<Map<String, Object>> listInquiries() {
        List<ContactInquiry> inquiry = contactInquiryService.getInquiryList();
        return ResponseEntity.ok(Map.of("inquiry", inquiry));
    }

    // 문의관리 API: 문의 상세 조회
    @GetMapping("/{id}")
    public ResponseEntity<?> getInquiry(@PathVariable("id") Long id) {
        ContactInquiry inquiry = contactInquiryService.getInquiry(id);
        if (inquiry == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", "문의 내역을 찾을 수 없습니다."));
        }
        return ResponseEntity.ok(Map.of("inquiry", inquiry));
    }

    // 고객문의 접수 API: 문의 등록
    @PostMapping
    public ResponseEntity<?> createInquiry(@RequestBody ContactInquiryCreateRequest request) {
        if (isBlank(request.getBusinessName()) || isBlank(request.getManagerName()) || isBlank(request.getPhoneNumber())
                || isBlank(request.getEmail()) || isBlank(request.getCurrentMealPrice())
                || isBlank(request.getDesiredMealPrice()) || isBlank(request.getDailyMealCount())
                || isBlank(request.getMealType()) || isBlank(request.getBusinessType())
                || isBlank(request.getTitle())
                || isBlank(request.getInquiryContent())) {
            return ResponseEntity.badRequest().body(Map.of("error", "필수 항목을 입력해 주세요."));
        }

        ContactInquiry saved = contactInquiryService.createInquiry(request);

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("message", "문의가 정상적으로 접수되었습니다. 확인 후 연락드리겠습니다.");
        payload.put("inquiryId", saved.getId());
        payload.put("inquiry", saved);
        return ResponseEntity.status(HttpStatus.CREATED).body(payload);
    }

    // 문의관리 API: 문의 답변 조회
    @GetMapping("/{id}/reply")
    public ResponseEntity<Map<String, Object>> getReply(@PathVariable("id") Long id) {
        ContactReply reply = contactInquiryService.getReply(id);
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("reply", reply);
        return ResponseEntity.ok(payload);
    }

    // 문의관리 API: 문의 답변 저장/수정
    @PostMapping("/{id}/reply")
    public ResponseEntity<?> upsertReply(@PathVariable("id") Long id, @RequestBody ContactReplyUpsertRequest request) {
        if (isBlank(request.getContent())) {
            return ResponseEntity.badRequest().body(Map.of("error", "답변 내용을 입력해 주세요."));
        }

        ContactReply savedReply = contactInquiryService.upsertReply(id, request);
        if (savedReply == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", "문의 내역을 찾을 수 없습니다."));
        }

        return ResponseEntity.ok(Map.of("message", "답변이 저장되었습니다.", "reply", savedReply));
    }

    // 문의관리 API: 문의 소프트삭제
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteInquiry(@PathVariable("id") Long id, @RequestBody(required = false) Map<String, String> body) {
        String deletedBy = body == null ? "admin" : body.getOrDefault("deletedBy", "admin");
        boolean deleted = contactInquiryService.softDeleteInquiry(id, deletedBy);
        if (!deleted) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", "문의 내역을 찾을 수 없습니다."));
        }
        return ResponseEntity.ok(new MessageResponse("문의가 삭제되었습니다."));
    }

    // 빈 문자열/공백 문자열 체크
    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}

