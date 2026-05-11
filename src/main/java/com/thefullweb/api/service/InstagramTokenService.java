package com.thefullweb.api.service;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import org.springframework.dao.DataAccessException;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.util.UriUtils;
import com.thefullweb.api.domain.instagram.InstagramToken;
import com.thefullweb.api.mapper.InstagramTokenMapper;

// 인스타그램 장기 토큰 조회와 만료 전 자동 갱신을 담당하는 서비스
@Service
public class InstagramTokenService {

    private static final long DEFAULT_EXPIRES_IN_SECONDS = 60L * 24L * 60L * 60L;
    private static final long REFRESH_THRESHOLD_DAYS = 14L;
    private static final ZoneId KST_ZONE_ID = ZoneId.of("Asia/Seoul");
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    private final InstagramTokenMapper instagramTokenMapper;
    private final RestClient restClient = RestClient.create();

    public InstagramTokenService(InstagramTokenMapper instagramTokenMapper) {
        this.instagramTokenMapper = instagramTokenMapper;
    }

    // DB 토큰을 우선 사용하고, 전환 전에는 properties 토큰을 대체값으로 사용
    public String getCurrentAccessToken(String fallbackAccessToken) {
        InstagramToken currentToken;
        try {
            currentToken = instagramTokenMapper.selectCurrentToken();
        } catch (DataAccessException ex) {
            return normalize(fallbackAccessToken);
        }

        String storedAccessToken = currentToken == null ? "" : normalize(currentToken.getAccessToken());

        if (!storedAccessToken.isEmpty()) {
            return storedAccessToken;
        }

        return normalize(fallbackAccessToken);
    }

    // 매일 새벽 인스타그램 장기 토큰 만료일을 확인하고 필요 시 갱신
    @Scheduled(cron = "${instagram.token-refresh-cron:0 0 3 * * *}", zone = "Asia/Seoul")
    @Transactional
    public void refreshTokenIfNeeded() {
        InstagramToken currentToken;
        try {
            currentToken = instagramTokenMapper.selectCurrentToken();
        } catch (DataAccessException ex) {
            return;
        }

        if (currentToken == null || normalize(currentToken.getAccessToken()).isEmpty()) {
            return;
        }

        LocalDateTime expiresAt = parseExpiresAt(currentToken.getExpiresAt());
        if (expiresAt != null && Duration.between(LocalDateTime.now(KST_ZONE_ID), expiresAt).toDays() > REFRESH_THRESHOLD_DAYS) {
            return;
        }

        InstagramRefreshResponse refreshResponse = requestRefreshToken(currentToken.getAccessToken());
        if (normalize(refreshResponse.accessToken()).isEmpty()) {
            throw new IllegalStateException("Instagram token refresh response is missing access token.");
        }

        long expiresInSeconds = refreshResponse.expiresIn() > 0
                ? refreshResponse.expiresIn()
                : DEFAULT_EXPIRES_IN_SECONDS;
        instagramTokenMapper.updateToken(currentToken.getId(), refreshResponse.accessToken(), expiresInSeconds);
    }

    // 새로 발급받은 장기 토큰을 DB에 저장할 때 사용하는 메서드
    @Transactional
    public void saveIssuedToken(String accessToken, long expiresInSeconds) {
        String normalizedAccessToken = normalize(accessToken);
        if (normalizedAccessToken.isEmpty()) {
            throw new IllegalArgumentException("Instagram access token is required.");
        }

        long normalizedExpiresInSeconds = expiresInSeconds > 0 ? expiresInSeconds : DEFAULT_EXPIRES_IN_SECONDS;
        InstagramToken currentToken = instagramTokenMapper.selectCurrentToken();
        if (currentToken == null) {
            instagramTokenMapper.insertToken(normalizedAccessToken, normalizedExpiresInSeconds);
            return;
        }

        instagramTokenMapper.updateToken(currentToken.getId(), normalizedAccessToken, normalizedExpiresInSeconds);
    }

    // 인스타그램 토큰 갱신 API 호출
    @SuppressWarnings("unchecked")
    private InstagramRefreshResponse requestRefreshToken(String accessToken) {
        String refreshUrl = "https://graph.instagram.com/refresh_access_token"
                + "?grant_type=ig_refresh_token"
                + "&access_token=" + UriUtils.encodeQueryParam(accessToken, StandardCharsets.UTF_8);

        try {
            Map<String, Object> response = restClient.get()
                    .uri(URI.create(refreshUrl))
                    .retrieve()
                    .body(Map.class);
            if (response == null) {
                return new InstagramRefreshResponse("", 0L);
            }

            return new InstagramRefreshResponse(
                    normalize(response.get("access_token")),
                    parseLong(response.get("expires_in")));
        } catch (RestClientResponseException ex) {
            throw new IllegalStateException("Instagram token refresh failed. " + normalize(ex.getResponseBodyAsString()));
        } catch (RestClientException ex) {
            throw new IllegalStateException("Instagram token refresh failed. " + normalize(ex.getMessage()));
        }
    }

    // DB 문자열 만료일을 LocalDateTime으로 변환
    private LocalDateTime parseExpiresAt(String value) {
        String normalizedValue = normalize(value);
        if (normalizedValue.isEmpty()) {
            return null;
        }

        return LocalDateTime.parse(normalizedValue, DATE_TIME_FORMATTER);
    }

    // 숫자형 API 응답 값을 long으로 변환
    private long parseLong(Object value) {
        if (value instanceof Number number) {
            return number.longValue();
        }

        try {
            return Long.parseLong(normalize(value));
        } catch (NumberFormatException ex) {
            return 0L;
        }
    }

    // 입력 문자열의 앞뒤 공백을 제거
    private String normalize(Object value) {
        return value == null ? "" : value.toString().trim();
    }

    // 인스타그램 토큰 갱신 API 응답
    private record InstagramRefreshResponse(String accessToken, long expiresIn) {
    }
}
