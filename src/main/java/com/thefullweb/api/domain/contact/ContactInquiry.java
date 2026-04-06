package com.thefullweb.api.domain.contact;

// 문의 본문/목록/상세에 공통 사용되는 데이터 모델
public class ContactInquiry {
    private Long id;
    private String businessName;
    private String managerName;
    private String phoneNumber;
    private String email;
    private String currentMealPrice;
    private String desiredMealPrice;
    private String dailyMealCount;
    private String mealType;
    private String businessType;
    private String switchingReason;
    private String title;
    private String inquiryContent;
    private String answerYn;
    private String submittedAt;
    private String source;
    private String erpSyncTarget;
    private String userId;
    private String modId;
    private String createdAt;
    private String updatedAt;
    private String delYn;
    private String delDt;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getBusinessName() {
        return businessName;
    }

    public void setBusinessName(String businessName) {
        this.businessName = businessName;
    }

    public String getManagerName() {
        return managerName;
    }

    public void setManagerName(String managerName) {
        this.managerName = managerName;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getCurrentMealPrice() {
        return currentMealPrice;
    }

    public void setCurrentMealPrice(String currentMealPrice) {
        this.currentMealPrice = currentMealPrice;
    }

    public String getDesiredMealPrice() {
        return desiredMealPrice;
    }

    public void setDesiredMealPrice(String desiredMealPrice) {
        this.desiredMealPrice = desiredMealPrice;
    }

    public String getDailyMealCount() {
        return dailyMealCount;
    }

    public void setDailyMealCount(String dailyMealCount) {
        this.dailyMealCount = dailyMealCount;
    }

    public String getMealType() {
        return mealType;
    }

    public void setMealType(String mealType) {
        this.mealType = mealType;
    }

    public String getBusinessType() {
        return businessType;
    }

    public void setBusinessType(String businessType) {
        this.businessType = businessType;
    }

    public String getSwitchingReason() {
        return switchingReason;
    }

    public void setSwitchingReason(String switchingReason) {
        this.switchingReason = switchingReason;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getInquiryContent() {
        return inquiryContent;
    }

    public void setInquiryContent(String inquiryContent) {
        this.inquiryContent = inquiryContent;
    }

    public String getAnswerYn() {
        return answerYn;
    }

    public void setAnswerYn(String answerYn) {
        this.answerYn = answerYn;
    }

    public String getSubmittedAt() {
        return submittedAt;
    }

    public void setSubmittedAt(String submittedAt) {
        this.submittedAt = submittedAt;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public String getErpSyncTarget() {
        return erpSyncTarget;
    }

    public void setErpSyncTarget(String erpSyncTarget) {
        this.erpSyncTarget = erpSyncTarget;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getModId() {
        return modId;
    }

    public void setModId(String modId) {
        this.modId = modId;
    }

    public String getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(String createdAt) {
        this.createdAt = createdAt;
    }

    public String getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(String updatedAt) {
        this.updatedAt = updatedAt;
    }

    public String getDelYn() {
        return delYn;
    }

    public void setDelYn(String delYn) {
        this.delYn = delYn;
    }

    public String getDelDt() {
        return delDt;
    }

    public void setDelDt(String delDt) {
        this.delDt = delDt;
    }
}

