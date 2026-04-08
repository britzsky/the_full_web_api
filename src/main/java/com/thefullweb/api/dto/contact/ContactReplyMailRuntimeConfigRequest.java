package com.thefullweb.api.dto.contact;

// 문의 답변 SMTP 런타임 설정 요청 DTO
public class ContactReplyMailRuntimeConfigRequest {
    private String userId;

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }
}
