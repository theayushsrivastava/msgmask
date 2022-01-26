package com.c2mtechnology.msgmask.Models;

public class AddressModel {
    String address;
    int spamCount,hamCount,reportCount,isReported, isOverridden,is_spammer;

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public int getSpamCount() {
        return spamCount;
    }

    public void setSpamCount(int spamCount) {
        this.spamCount = spamCount;
    }

    public int getHamCount() {
        return hamCount;
    }

    public void setHamCount(int hamCount) {
        this.hamCount = hamCount;
    }

    public int getReportCount() {
        return reportCount;
    }

    public void setReportCount(int reportCount) {
        this.reportCount = reportCount;
    }

    public int getIsReported() {
        return isReported;
    }

    public void setIsReported(int isReported) {
        this.isReported = isReported;
    }

    public int getIsOverridden() {
        return isOverridden;
    }

    public void setIsOverridden(int isOverridden) {
        this.isOverridden = isOverridden;
    }

    public int getIs_spammer() {
        return is_spammer;
    }

    public void setIs_spammer(int is_spammer) {
        this.is_spammer = is_spammer;
    }
}
