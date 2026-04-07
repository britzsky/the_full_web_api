package com.thefullweb.api.controller;

import java.net.URI;
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
import jakarta.servlet.http.HttpServletRequest;
import com.thefullweb.api.domain.contact.ContactInquiry;
import com.thefullweb.api.domain.contact.ContactReply;
import com.thefullweb.api.dto.contact.ContactInquiryCreateRequest;
import com.thefullweb.api.dto.contact.ContactReplyUpsertRequest;
import com.thefullweb.api.dto.common.MessageResponse;
import com.thefullweb.api.service.ContactInquiryErpService;
import com.thefullweb.api.service.ContactInquiryService;

import com.thefullweb.api.config.WebCorsConfig;

// 고객문의/문의관리 API 컨트롤러
@RestController
@RequestMapping("/contact/manage")
public class ContactInquiryController {

    // 문의 도메인 서비스 주입
    private final ContactInquiryService contactInquiryService;
    private final ContactInquiryErpService contactInquiryErpService;
	private WebCorsConfig webCorsConfig;

    public ContactInquiryController(
            ContactInquiryService contactInquiryService,
            WebCorsConfig webCorsConfig,
            ContactInquiryErpService contactInquiryErpService) {
		        this.contactInquiryService = contactInquiryService;
		        this.contactInquiryErpService = contactInquiryErpService;
		        this.webCorsConfig = webCorsConfig;
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
        if (isBlank(request.getBusinessName()) || isBlank(request.getManagerName()) || isBlank(request.getPhoneNumber())
                || isBlank(request.getEmail()) || isBlank(request.getCurrentMealPrice())
                || isBlank(request.getDesiredMealPrice()) || isBlank(request.getDailyMealCount())
                || isBlank(request.getMealType()) || isBlank(request.getBusinessType())
                || isBlank(request.getTitle())
                || isBlank(request.getInquiryContent())) {
            return ResponseEntity.badRequest().body(Map.of("error", "필수 항목을 입력해 주세요."));
        }

        ContactInquiry saved = contactInquiryService.createInquiry(request);
        Map<String, Object> erpSync = contactInquiryErpService.notifyInquiryCreated(
                saved,
                resolvePublicWebBaseUrl(httpServletRequest));

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
}

