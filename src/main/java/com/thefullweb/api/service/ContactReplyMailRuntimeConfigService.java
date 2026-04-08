package com.thefullweb.api.service;

import java.nio.charset.StandardCharsets;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import com.thefullweb.api.dto.contact.ContactReplyMailRuntimeConfigRequest;
import com.thefullweb.api.dto.contact.ContactReplyMailRuntimeConfigResponse;

// 문의 답변용 SMTP 런타임 설정 생성 서비스
@Service
public class ContactReplyMailRuntimeConfigService {

    @Value("${contact.reply.smtp.host:smtp.zioyou.com}")
    private String smtpHost;

    @Value("${contact.reply.smtp.port:465}")
    private Integer smtpPort;

    @Value("${contact.reply.smtp.secure:true}")
    private Boolean smtpSecure;

    @Value("${contact.reply.smtp.user-domain:@thefull.co.kr}")
    private String smtpUserDomain;

    @Value("${contact.reply.mail.from-display-name:더채움}")
    private String mailFromDisplayName;

    // 문의 답변 user_id 기준 SMTP 계정 정보를 생성
    public ContactReplyMailRuntimeConfigResponse resolve(ContactReplyMailRuntimeConfigRequest request) {
        String userId = normalize(request == null ? null : request.getUserId());

        if (userId.isEmpty()) {
            throw new IllegalArgumentException("SMTP user_id를 입력해 주세요.");
        }

        String smtpUser = buildSmtpUser(userId);

        ContactReplyMailRuntimeConfigResponse response = new ContactReplyMailRuntimeConfigResponse();
        response.setSmtpHost(normalize(smtpHost));
        response.setSmtpPort(smtpPort == null || smtpPort.intValue() <= 0 ? Integer.valueOf(465) : smtpPort);
        response.setSmtpSecure(smtpSecure == null ? Boolean.TRUE : smtpSecure);
        response.setSmtpUser(smtpUser);
        response.setMailFrom(buildMailFrom(smtpUser));
        response.setMailReplyTo(smtpUser);
        return response;
    }

    // user_id를 메일 로그인 계정 주소로 정규화
    private String buildSmtpUser(String userId) {
        String normalizedUserId = normalize(userId);
        if (normalizedUserId.contains("@")) {
            return normalizedUserId;
        }

        String normalizedDomain = normalize(smtpUserDomain);
        if (normalizedDomain.isEmpty()) {
            return normalizedUserId;
        }

        return normalizedDomain.startsWith("@")
                ? normalizedUserId + normalizedDomain
                : normalizedUserId + "@" + normalizedDomain;
    }

    // 발신자 표시명과 메일 주소를 결합
    private String buildMailFrom(String smtpUser) {
        String displayName = resolveDisplayName(mailFromDisplayName);
        return displayName.isEmpty() ? smtpUser : displayName + " <" + smtpUser + ">";
    }

    // properties 인코딩 차이로 한글 표시명이 깨졌으면 UTF-8 문자열로 복원
    private String resolveDisplayName(String value) {
        String normalized = normalize(value);
        if (normalized.isEmpty()) {
            return "";
        }

        if (containsHangul(normalized) || isAsciiOnly(normalized)) {
            return normalized;
        }

        String repaired = new String(normalized.getBytes(StandardCharsets.ISO_8859_1), StandardCharsets.UTF_8).trim();
        return containsHangul(repaired) ? repaired : normalized;
    }

    // 한글 음절이 하나라도 포함되었는지 확인
    private boolean containsHangul(String value) {
        return value != null && value.codePoints().anyMatch(codePoint -> codePoint >= 0xAC00 && codePoint <= 0xD7A3);
    }

    // ASCII 범위만 있으면 인코딩 복원을 시도하지 않음
    private boolean isAsciiOnly(String value) {
        return value != null && value.chars().allMatch(codePoint -> codePoint >= 0x20 && codePoint <= 0x7E);
    }

    // 문자열 공백 제거
    private String normalize(String value) {
        return value == null ? "" : value.trim();
    }
}
