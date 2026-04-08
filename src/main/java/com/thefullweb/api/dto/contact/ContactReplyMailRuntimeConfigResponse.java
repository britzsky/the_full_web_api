package com.thefullweb.api.dto.contact;

// 문의 답변 SMTP 런타임 설정 응답 DTO
public class ContactReplyMailRuntimeConfigResponse {
    private String smtpHost;
    private Integer smtpPort;
    private Boolean smtpSecure;
    private String smtpUser;
    private String mailFrom;
    private String mailReplyTo;

    public String getSmtpHost() {
        return smtpHost;
    }

    public void setSmtpHost(String smtpHost) {
        this.smtpHost = smtpHost;
    }

    public Integer getSmtpPort() {
        return smtpPort;
    }

    public void setSmtpPort(Integer smtpPort) {
        this.smtpPort = smtpPort;
    }

    public Boolean getSmtpSecure() {
        return smtpSecure;
    }

    public void setSmtpSecure(Boolean smtpSecure) {
        this.smtpSecure = smtpSecure;
    }

    public String getSmtpUser() {
        return smtpUser;
    }

    public void setSmtpUser(String smtpUser) {
        this.smtpUser = smtpUser;
    }

    public String getMailFrom() {
        return mailFrom;
    }

    public void setMailFrom(String mailFrom) {
        this.mailFrom = mailFrom;
    }

    public String getMailReplyTo() {
        return mailReplyTo;
    }

    public void setMailReplyTo(String mailReplyTo) {
        this.mailReplyTo = mailReplyTo;
    }
}
