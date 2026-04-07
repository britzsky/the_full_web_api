package com.thefullweb.api.service;

import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;
import com.thefullweb.api.domain.contact.ContactInquiry;
import com.thefullweb.api.domain.contact.ContactReply;

// 고객문의 등록/답변 저장 이후 ERP 웹훅 알림을 전송하는 서비스
@Service
public class ContactInquiryErpService {

    private static final String DEFAULT_CONTACT_ROUTE_USER_ID = "ww1";

    // ERP 웹훅 호출용 HTTP 클라이언트
    private final RestClient restClient = RestClient.create();

    // 고객문의 ERP 웹훅 주소
    @Value("${erp.contact.webhook-url:}")
    private String contactWebhookUrl;

    // ERP 웹훅 인증 시크릿
    @Value("${erp.webhook.secret:}")
    private String webhookSecret;

    // 라우팅 우선순위 설정
    @Value("${erp.contact.route.priority:user_id}")
    private String routePriority;

    // 라우팅 대상 user_id 목록
    @Value("${erp.contact.route.user-ids:}")
    private String routeUserIds;

    // 라우팅 대상 직책 유형 목록
    @Value("${erp.contact.route.position-types:}")
    private String routePositionTypes;

    // 라우팅 대상 부서 목록
    @Value("${erp.contact.route.departments:}")
    private String routeDepartments;

    // 문의 등록 ERP 알림 전송
    public Map<String, Object> notifyInquiryCreated(ContactInquiry inquiry, String publicWebBaseUrl) {
        List<String> configuredUserIds = resolveCsvTexts(routeUserIds);
        List<String> targetUserIds = configuredUserIds.isEmpty()
                ? List.of(DEFAULT_CONTACT_ROUTE_USER_ID)
                : configuredUserIds;
        String primaryRouteUserId = targetUserIds.get(0);

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("id", inquiry.getId());
        payload.put("title", normalize(inquiry.getTitle()));
        payload.put("business_name", normalize(inquiry.getBusinessName()));
        payload.put("manager_name", normalize(inquiry.getManagerName()));
        payload.put("phone_number", normalize(inquiry.getPhoneNumber()));
        payload.put("email", normalize(inquiry.getEmail()));
        payload.put("answer_yn", normalize(inquiry.getAnswerYn()));
        payload.put("submitted_at", firstNonBlank(inquiry.getSubmittedAt(), inquiry.getCreatedAt()));
        payload.put("source", normalize(inquiry.getSource()));
        payload.put("erp_sync_target", normalize(inquiry.getErpSyncTarget()));
        payload.put("user_id", primaryRouteUserId);
        payload.put("target_user_id", primaryRouteUserId);
        payload.put("target_user_ids", targetUserIds);
        payload.put("manage_url", buildContactManageUrl(publicWebBaseUrl, inquiry.getId(), ""));

        return sendNotification("CONTACT_INQUIRY_CREATED", payload, List.of());
    }

    // 문의 답변 저장 ERP 알림 전송
    public Map<String, Object> notifyInquiryReplied(
            ContactInquiry inquiry,
            ContactReply reply,
            String fallbackUserId,
            String publicWebBaseUrl) {
        String replyUserId = firstNonBlank(reply.getUserId(), fallbackUserId, "admin");

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("id", inquiry.getId());
        payload.put("inquiry_id", inquiry.getId());
        payload.put("title", normalize(inquiry.getTitle()));
        payload.put("business_name", normalize(inquiry.getBusinessName()));
        payload.put("manager_name", normalize(inquiry.getManagerName()));
        payload.put("phone_number", normalize(inquiry.getPhoneNumber()));
        payload.put("email", normalize(inquiry.getEmail()));
        payload.put("answer_yn", normalize(inquiry.getAnswerYn()));
        payload.put("reply_user_id", replyUserId);
        payload.put("user_id", replyUserId);
        payload.put("target_user_id", replyUserId);
        payload.put("replied_at", firstNonBlank(reply.getModifiedAt(), reply.getRegisteredAt()));
        payload.put("manage_url", buildContactManageUrl(publicWebBaseUrl, inquiry.getId(), replyUserId));

        return sendNotification("CONTACT_INQUIRY_REPLIED", payload, List.of(replyUserId));
    }

    // ERP 웹훅 공통 요청 전송
    private Map<String, Object> sendNotification(
            String eventType,
            Map<String, Object> payload,
            List<String> preferredUserIds) {
        String webhookUrl = normalize(contactWebhookUrl);
        if (webhookUrl.isEmpty()) {
            return buildSyncResult(false, "erp_webhook_not_configured", null);
        }

        List<String> mergedUserIds = mergeUserIds(preferredUserIds, resolveCsvTexts(routeUserIds));
        if (mergedUserIds.isEmpty()) {
            mergedUserIds.add(DEFAULT_CONTACT_ROUTE_USER_ID);
        }

        List<String> priorities = resolveCsvTexts(routePriority);
        if (priorities.isEmpty()) {
            priorities = List.of("user_id");
        }

        Map<String, Object> routingHints = new LinkedHashMap<>();
        routingHints.put("priority", priorities);
        routingHints.put("userIds", mergedUserIds);

        List<Integer> positionTypes = resolveCsvNumbers(routePositionTypes);
        if (!positionTypes.isEmpty()) {
            routingHints.put("positionTypes", positionTypes);
        }

        List<Integer> departments = resolveCsvNumbers(routeDepartments);
        if (!departments.isEmpty()) {
            routingHints.put("departments", departments);
        }

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("eventType", eventType);
        body.put("sourceSystem", "the_full_web");
        body.put("payload", payload);
        body.put("routingHints", routingHints);

        try {
            restClient.post()
                    .uri(URI.create(webhookUrl))
                    .headers((headers) -> {
                        headers.set("Content-Type", "application/json");
                        if (!normalize(webhookSecret).isEmpty()) {
                            headers.set("X-ERP-WEBHOOK-SECRET", normalize(webhookSecret));
                        }
                    })
                    .body(body)
                    .retrieve()
                    .toBodilessEntity();

            return buildSyncResult(true, "", null);
        } catch (RestClientResponseException ex) {
            return buildSyncResult(false, "erp_webhook_failed", ex.getStatusCode().value());
        } catch (RestClientException | IllegalArgumentException ex) {
            return buildSyncResult(false, "erp_webhook_exception", null);
        }
    }

    // 문의관리 상세 화면 링크 조합
    private String buildContactManageUrl(String publicWebBaseUrl, Long inquiryId, String erpUserId) {
        String baseUrl = normalize(publicWebBaseUrl);
        if (baseUrl.isEmpty()) {
            baseUrl = "http://localhost:8081";
        }
        baseUrl = baseUrl.replaceAll("/+$", "");

        String route = baseUrl + "/contact/manage";
        if (inquiryId != null && inquiryId > 0) {
            route += "/" + inquiryId;
        }

        String normalizedErpUserId = normalize(erpUserId);
        if (normalizedErpUserId.isEmpty()) {
            return route;
        }

        return route + "?erp_user_id=" + URLEncoder.encode(normalizedErpUserId, StandardCharsets.UTF_8);
    }

    // CSV 문자열을 공백 제거/중복 제거된 문자열 목록으로 변환
    private List<String> resolveCsvTexts(String value) {
        List<String> parsed = new ArrayList<>();

        for (String token : normalize(value).split(",")) {
            String normalizedToken = normalize(token);
            if (normalizedToken.isEmpty() || parsed.contains(normalizedToken)) {
                continue;
            }
            parsed.add(normalizedToken);
        }

        return parsed;
    }

    // CSV 문자열을 정수 목록으로 변환
    private List<Integer> resolveCsvNumbers(String value) {
        List<Integer> parsed = new ArrayList<>();

        for (String token : resolveCsvTexts(value)) {
            try {
                Integer number = Integer.valueOf(token);
                if (!parsed.contains(number)) {
                    parsed.add(number);
                }
            } catch (NumberFormatException ignored) {
                // 숫자가 아닌 라우팅 설정값은 무시
            }
        }

        return parsed;
    }

    // 우선 대상 user_id와 기본 라우팅 user_id를 합쳐 중복 없이 정리
    private List<String> mergeUserIds(List<String> preferredUserIds, List<String> configuredUserIds) {
        List<String> merged = new ArrayList<>();

        for (String userId : preferredUserIds) {
            String normalizedUserId = normalize(userId);
            if (!normalizedUserId.isEmpty() && !merged.contains(normalizedUserId)) {
                merged.add(normalizedUserId);
            }
        }

        for (String userId : configuredUserIds) {
            String normalizedUserId = normalize(userId);
            if (!normalizedUserId.isEmpty() && !merged.contains(normalizedUserId)) {
                merged.add(normalizedUserId);
            }
        }

        return merged;
    }

    // ERP 동기화 결과 응답 형태 구성
    private Map<String, Object> buildSyncResult(boolean queued, String reason, Integer status) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("queued", queued);

        String normalizedReason = normalize(reason);
        if (!normalizedReason.isEmpty()) {
            result.put("reason", normalizedReason);
        }

        if (status != null && status > 0) {
            result.put("status", status);
        }

        return result;
    }

    // 여러 후보 문자열 중 첫 번째 유효값 반환
    private String firstNonBlank(String... values) {
        for (String value : values) {
            String normalized = normalize(value);
            if (!normalized.isEmpty()) {
                return normalized;
            }
        }
        return "";
    }

    // 문자열 입력값 공백 제거
    private String normalize(Object value) {
        return value == null ? "" : value.toString().trim();
    }
}
