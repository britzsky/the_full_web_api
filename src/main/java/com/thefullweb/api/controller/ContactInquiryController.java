package com.thefullweb.api.controller;

import java.net.URI;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import jakarta.servlet.http.HttpServletRequest;
import com.thefullweb.api.domain.contact.ContactInquiry;
import com.thefullweb.api.domain.contact.ContactReply;
import com.thefullweb.api.dto.contact.ContactInquiryCreateRequest;
import com.thefullweb.api.dto.contact.ContactReplyMailRuntimeConfigRequest;
import com.thefullweb.api.dto.contact.ContactReplyMailRuntimeConfigResponse;
import com.thefullweb.api.dto.contact.ContactReplyUpsertRequest;
import com.thefullweb.api.dto.common.MessageResponse;
import com.thefullweb.api.mapper.ContactInquiryMapper;
import com.thefullweb.api.service.ContactInquiryErpService;
import com.thefullweb.api.service.ContactInquiryService;
import com.thefullweb.api.service.ContactReplyMailRuntimeConfigService;

// 고객문의/문의관리 API 컨트롤러
@RestController
@RequestMapping("/contact/manage")
public class ContactInquiryController {

    private static final List<String> MEAL_TYPE_OPTIONS = List.of("조식", "중식", "석식", "기타");
    private static final String MEAL_TYPE_OTHER = "기타";
    private static final int MEAL_TYPE_OTHER_MAX_LENGTH = 50;

    // 문의 도메인 서비스 주입
    private final ContactInquiryService contactInquiryService;
    private final ContactInquiryErpService contactInquiryErpService;
    private final ContactReplyMailRuntimeConfigService contactReplyMailRuntimeConfigService;
    private final ContactInquiryMapper contactInquiryMapper;
    private final String internalApiSecret;

    public ContactInquiryController(
            ContactInquiryService contactInquiryService,
            ContactInquiryErpService contactInquiryErpService,
            ContactReplyMailRuntimeConfigService contactReplyMailRuntimeConfigService,
            ContactInquiryMapper contactInquiryMapper,
            @Value("${erp.internal-api.secret:}") String internalApiSecret) {
        this.contactInquiryService = contactInquiryService;
        this.contactInquiryErpService = contactInquiryErpService;
        this.contactReplyMailRuntimeConfigService = contactReplyMailRuntimeConfigService;
        this.contactInquiryMapper = contactInquiryMapper;
        this.internalApiSecret = internalApiSecret == null ? "" : internalApiSecret.trim();
    }

    // 내부 API: 답변 메일 발송용 ERP 사용자 계정 조회
    @GetMapping("/user/mail-auth")
    public ResponseEntity<?> getUserMailAuth(
            @RequestParam("user_id") String userId,
            @RequestHeader(value = "X-THEFULL-INTERNAL-SECRET", required = false) String requestSecret) {
        String normalizedSecret = normalize(requestSecret);
        if (!internalApiSecret.isEmpty() && !normalizedSecret.isEmpty() && !internalApiSecret.equals(normalizedSecret)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", "내부 연동 전용 API입니다."));
        }

        String normalizedUserId = normalize(userId);
        if (normalizedUserId.isEmpty() || !normalizedUserId.matches("^[A-Za-z0-9._-]{1,40}$")) {
            return ResponseEntity.badRequest().body(Map.of("error", "user_id가 올바르지 않습니다."));
        }

        String password = contactInquiryMapper.selectErpUserMailAuthPassword(normalizedUserId);
        if (password == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", "사용자 정보를 찾을 수 없습니다."));
        }

        return ResponseEntity.ok(Map.of(
                "user_id", normalizedUserId,
                "password", password));
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
    public ResponseEntity<?> createInquiry(
            @RequestBody ContactInquiryCreateRequest request,
            HttpServletRequest httpServletRequest) {
        // DB 컬럼이 nullable로 변경되어 필수값은 5개(업장명/담당자/연락처/이메일/문의내용)만 검증한다.
        if (isBlank(request.getBusinessName()) || isBlank(request.getManagerName()) || isBlank(request.getPhoneNumber())
                || isBlank(request.getEmail()) || isBlank(request.getInquiryContent())) {
            return ResponseEntity.badRequest().body(Map.of("error", "필수 항목을 입력해 주세요."));
        }

        String mealTypeValidationError = validateMealTypes(request);
        if (!mealTypeValidationError.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", mealTypeValidationError));
        }

        ContactInquiry saved = contactInquiryService.createInquiry(request);
        Map<String, Object> erpSync = contactInquiryErpService.notifyInquiryCreated(
                saved,
                resolvePublicWebBaseUrl(httpServletRequest));
        String assignedUserId = normalize(erpSync.get("primary_user_id"));
        if (!assignedUserId.isEmpty()) {
            ContactInquiry assignedInquiry = contactInquiryService.assignInquiryUser(saved.getId(), assignedUserId);
            if (assignedInquiry != null) {
                saved = assignedInquiry;
            }
        }

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("message", "문의가 정상적으로 접수되었습니다. 확인 후 연락드리겠습니다.");
        payload.put("inquiryId", saved.getId());
        payload.put("inquiry", saved);
        payload.put("erpSync", erpSync);
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

    // 문의 답변 메일 발송용 SMTP 런타임 설정 조회
    @PostMapping("/reply/mail-runtime-config")
    public ResponseEntity<?> getReplyMailRuntimeConfig(@RequestBody ContactReplyMailRuntimeConfigRequest request) {
        try {
            ContactReplyMailRuntimeConfigResponse config = contactReplyMailRuntimeConfigService.resolve(request);
            return ResponseEntity.ok(Map.of("config", config));
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(Map.of("error", ex.getMessage()));
        }
    }

    // 문의관리 API: 문의 답변 저장/수정
    @PostMapping("/{id}/reply")
    public ResponseEntity<?> upsertReply(
            @PathVariable("id") Long id,
            @RequestBody ContactReplyUpsertRequest request,
            HttpServletRequest httpServletRequest) {
        if (isBlank(request.getContent())) {
            return ResponseEntity.badRequest().body(Map.of("error", "답변 내용을 입력해 주세요."));
        }

        ContactReply savedReply = contactInquiryService.upsertReply(id, request);
        if (savedReply == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", "문의 내역을 찾을 수 없습니다."));
        }

        ContactInquiry inquiry = contactInquiryService.getInquiry(id);
        Map<String, Object> erpSync = inquiry == null
                ? Map.of("queued", false, "reason", "inquiry_not_found_after_reply_save")
                : contactInquiryErpService.notifyInquiryReplied(
                        inquiry,
                        savedReply,
                        request.getUserId(),
                        resolvePublicWebBaseUrl(httpServletRequest));

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("message", "답변이 저장되었습니다.");
        payload.put("reply", savedReply);
        payload.put("erpSync", erpSync);
        return ResponseEntity.ok(payload);
    }

    // 문의관리 API: 답변 메일 발송 완료 상태 반영
    @PostMapping("/{id}/reply/complete")
    public ResponseEntity<?> completeReply(
            @PathVariable("id") Long id,
            @RequestBody(required = false) Map<String, String> body) {
        ContactReply reply = contactInquiryService.markReplyEmailSent(id);
        if (reply == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", "저장된 답변을 찾을 수 없습니다."));
        }

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("message", "이메일 발송 상태가 반영되었습니다.");
        payload.put("reply", reply);
        return ResponseEntity.ok(payload);
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

    // 복수 선택 식사 구분과 기타 직접 입력값의 저장 가능 여부 확인
    private String validateMealTypes(ContactInquiryCreateRequest request) {
        List<String> mealTypes = request.getMealTypes();
        if (mealTypes == null) {
            // 기존 단일 문자열 요청과 식사 구분이 없는 간편문의 요청은 그대로 허용한다.
            return "";
        }
        if (mealTypes.isEmpty()) {
            return "식사 구분을 하나 이상 선택해 주세요.";
        }

        boolean hasOther = false;
        for (String mealType : mealTypes) {
            String normalizedMealType = normalize(mealType);
            if (!MEAL_TYPE_OPTIONS.contains(normalizedMealType)) {
                return "식사 구분 선택값이 올바르지 않습니다.";
            }
            if (MEAL_TYPE_OTHER.equals(normalizedMealType)) {
                hasOther = true;
            }
        }

        String mealTypeOther = normalize(request.getMealTypeOther());
        if (hasOther && mealTypeOther.isEmpty()) {
            return "기타 식사 구분을 입력해 주세요.";
        }
        if (mealTypeOther.length() > MEAL_TYPE_OTHER_MAX_LENGTH) {
            return "기타 식사 구분은 50자 이내로 입력해 주세요.";
        }
        return "";
    }

    // 브라우저 Origin/Referer를 기준으로 문의관리 화면 베이스 주소 추출
    private String resolvePublicWebBaseUrl(HttpServletRequest request) {
        String origin = normalize(request.getHeader("Origin"));
        if (isHttpUrl(origin)) {
            return origin.replaceAll("/+$", "");
        }

        String referer = normalize(request.getHeader("Referer"));
        if (referer.isEmpty()) {
            return "";
        }

        try {
            URI uri = URI.create(referer);
            String scheme = normalize(uri.getScheme());
            String host = normalize(uri.getHost());
            if (!("http".equalsIgnoreCase(scheme) || "https".equalsIgnoreCase(scheme)) || host.isEmpty()) {
                return "";
            }

            return uri.getPort() > 0
                    ? scheme + "://" + host + ":" + uri.getPort()
                    : scheme + "://" + host;
        } catch (IllegalArgumentException ex) {
            return "";
        }
    }

    // HTTP/HTTPS 절대 주소 여부 확인
    private boolean isHttpUrl(String value) {
        String normalized = normalize(value);
        if (normalized.isEmpty()) {
            return false;
        }

        try {
            URI uri = URI.create(normalized);
            String scheme = normalize(uri.getScheme());
            return ("http".equalsIgnoreCase(scheme) || "https".equalsIgnoreCase(scheme))
                    && !normalize(uri.getHost()).isEmpty();
        } catch (IllegalArgumentException ex) {
            return false;
        }
    }

    // 헤더 문자열 공백 제거
    private String normalize(String value) {
        return value == null ? "" : value.trim();
    }

    // Object 타입 문자열 공백 제거
    private String normalize(Object value) {
        return value == null ? "" : value.toString().trim();
    }
}

