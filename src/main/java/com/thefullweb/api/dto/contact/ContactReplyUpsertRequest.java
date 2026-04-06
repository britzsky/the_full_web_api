package com.thefullweb.api.dto.contact;

// 문의 답변 저장/수정 요청 DTO
public class ContactReplyUpsertRequest {
    private String content;
    private String userId;

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }
}

