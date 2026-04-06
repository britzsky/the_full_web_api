package com.thefullweb.api.dto.contact;

// 고객문의 등록 요청 DTO
public class ContactInquiryCreateRequest {
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
    private String submittedAt;
    private String source;
    private String erpSyncTarget;

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
}

