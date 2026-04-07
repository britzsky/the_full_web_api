package com.thefullweb.api.controller;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.util.UriUtils;

// 서버 상태 확인과 공개 조회성 엔드포인트를 처리하는 컨트롤러
@RestController
public class HealthController {

    // 외부 공개 API 호출용 RestClient
    private final RestClient restClient = RestClient.create();

    // 로컬/운영 시크릿 설정에서 읽는 인스타그램 access token
    @Value("${instagram.access-token:}")
    private String accessToken;

    // API 정상 기동 여부 응답
    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> health() {
        return ResponseEntity.ok(Map.of("status", "ok", "service", "the_full_web_api"));
    }

    // 홈 소셜 섹션용 인스타그램 사용자 정보와 최신 미디어 목록 조회
    @GetMapping("/instagram")
    public ResponseEntity<Map<String, Object>> getInstagramFeed() {
        try {
            return ResponseEntity.ok(loadInstagramFeed());
        } catch (IllegalStateException ex) {
            Map<String, Object> payload = new LinkedHashMap<>();
            payload.put("error", ex.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(payload);
        } catch (InstagramApiException ex) {
            Map<String, Object> payload = new LinkedHashMap<>();
            payload.put("error", ex.getMessage());

            if (!ex.getDetails().isBlank()) {
                payload.put("details", ex.getDetails());
            }

            return ResponseEntity.status(ex.getStatusCode()).body(payload);
        }
    }

    // 인스타그램 사용자 정보와 최신 미디어 목록을 조합해서 반환
    private Map<String, Object> loadInstagramFeed() {
        String normalizedAccessToken = normalize(accessToken);
        if (normalizedAccessToken.isEmpty()) {
            throw new IllegalStateException("Missing Instagram access token.");
        }

        Map<String, Object> meData = null;
        HttpStatusCode meErrorStatus = HttpStatus.INTERNAL_SERVER_ERROR;
        String meErrorText = "";
        String[] meFieldsCandidates = {
                "id,username,media_count,profile_picture_url",
                "id,username,media_count"
        };

        for (String fields : meFieldsCandidates) {
            try {
                meData = requestMap(buildInstagramApiUrl("/me", fields, null, normalizedAccessToken));
                break;
            } catch (InstagramApiException ex) {
                meErrorStatus = ex.getStatusCode();
                meErrorText = ex.getDetails();
            }
        }

        if (meData == null) {
            throw new InstagramApiException(meErrorStatus, "Failed to fetch Instagram user.", meErrorText);
        }

        String userId = normalize(meData.get("id"));
        if (userId.isEmpty()) {
            throw new InstagramApiException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "Failed to fetch Instagram user.",
                    "Instagram user id is missing.");
        }

        Map<String, Object> mediaData;
        try {
            mediaData = requestMap(buildInstagramApiUrl(
                    "/" + userId + "/media",
                    "id,caption,media_type,media_url,thumbnail_url,permalink,timestamp,children{id,media_type,media_url,thumbnail_url}",
                    8,
                    normalizedAccessToken));
        } catch (InstagramApiException ex) {
            throw new InstagramApiException(ex.getStatusCode(), "Failed to fetch Instagram media.", ex.getDetails());
        }

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("user", meData);
        payload.put("data", toMediaItems(mediaData.get("data")));
        return payload;
    }

    // 인스타그램 API JSON 응답을 Map 형태로 수신
    @SuppressWarnings("unchecked")
    private Map<String, Object> requestMap(String url) {
        try {
            Map<String, Object> response = restClient.get()
                    .uri(URI.create(url))
                    .retrieve()
                    .body(Map.class);
            return response == null ? Collections.emptyMap() : response;
        } catch (RestClientResponseException ex) {
            throw new InstagramApiException(
                    ex.getStatusCode(),
                    "Instagram API request failed.",
                    normalize(ex.getResponseBodyAsString()));
        } catch (RestClientException ex) {
            throw new InstagramApiException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "Instagram API request failed.",
                    normalize(ex.getMessage()));
        }
    }

    // 인스타그램 Graph API URL 문자열 조합
    private String buildInstagramApiUrl(String path, String fields, Integer limit, String token) {
        StringBuilder urlBuilder = new StringBuilder("https://graph.instagram.com")
                .append(path)
                .append("?fields=")
                .append(UriUtils.encodeQueryParam(fields, StandardCharsets.UTF_8));

        if (limit != null) {
            urlBuilder.append("&limit=").append(limit);
        }

        return urlBuilder.append("&access_token=")
                .append(UriUtils.encodeQueryParam(token, StandardCharsets.UTF_8))
                .toString();
    }

    // 인스타그램 미디어 배열을 프론트에서 사용하는 형태로 정규화
    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> toMediaItems(Object value) {
        if (!(value instanceof List<?> mediaItems)) {
            return Collections.emptyList();
        }

        return mediaItems.stream()
                .filter((item) -> item instanceof Map<?, ?>)
                .map((item) -> normalizeMediaItem((Map<String, Object>) item))
                .toList();
    }

    // 캐러셀 자식 미디어를 포함한 게시물 1건 응답 정규화
    private Map<String, Object> normalizeMediaItem(Map<String, Object> mediaItem) {
        Map<String, Object> normalized = new LinkedHashMap<>();
        normalized.put("id", mediaItem.get("id"));
        normalized.put("caption", mediaItem.get("caption"));
        normalized.put("media_type", mediaItem.get("media_type"));
        normalized.put("media_url", mediaItem.get("media_url"));
        normalized.put("thumbnail_url", mediaItem.get("thumbnail_url"));
        normalized.put("permalink", mediaItem.get("permalink"));
        normalized.put("timestamp", mediaItem.get("timestamp"));
        normalized.put("children", toMediaChildren(extractChildrenData(mediaItem.get("children"))));
        return normalized;
    }

    // children.data 배열만 추출
    @SuppressWarnings("unchecked")
    private Object extractChildrenData(Object value) {
        if (!(value instanceof Map<?, ?> childrenWrapper)) {
            return Collections.emptyList();
        }

        return ((Map<String, Object>) childrenWrapper).get("data");
    }

    // 캐러셀 자식 미디어 배열 정규화
    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> toMediaChildren(Object value) {
        if (!(value instanceof List<?> mediaChildren)) {
            return Collections.emptyList();
        }

        return mediaChildren.stream()
                .filter((item) -> item instanceof Map<?, ?>)
                .map((item) -> (Map<String, Object>) item)
                .filter((item) -> !normalize(item.get("media_url")).isEmpty())
                .map((item) -> {
                    Map<String, Object> normalized = new LinkedHashMap<>();
                    normalized.put("id", item.get("id"));
                    normalized.put("media_type", item.get("media_type"));
                    normalized.put("media_url", item.get("media_url"));
                    normalized.put("thumbnail_url", item.get("thumbnail_url"));
                    return normalized;
                })
                .toList();
    }

    // 문자열 입력값 공백 정리
    private String normalize(Object value) {
        return value == null ? "" : value.toString().trim();
    }

    // 인스타그램 API 오류 상태와 상세 내용을 전달하는 예외
    private static class InstagramApiException extends RuntimeException {

        private final HttpStatusCode statusCode;
        private final String details;

        private InstagramApiException(HttpStatusCode statusCode, String message, String details) {
            super(message);
            this.statusCode = statusCode;
            this.details = details == null ? "" : details;
        }

        private HttpStatusCode getStatusCode() {
            return statusCode;
        }

        private String getDetails() {
            return details;
        }
    }
}

