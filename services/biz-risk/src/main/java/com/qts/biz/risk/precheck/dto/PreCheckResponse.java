package com.qts.biz.risk.precheck.dto;

import java.util.ArrayList;
import java.util.List;

/**
 * Pre-check response DTO
 * Returns validation result with reasons if rejected
 */
public class PreCheckResponse {

    private int code;
    private String message;
    private PreCheckData data;

    public PreCheckResponse() {
        this.data = new PreCheckData();
    }

    public static PreCheckResponse success() {
        PreCheckResponse response = new PreCheckResponse();
        response.setCode(0);
        response.setMessage("success");
        response.getData().setCanTrade(true);
        return response;
    }

    public static PreCheckResponse failure(String reason) {
        PreCheckResponse response = new PreCheckResponse();
        response.setCode(1);
        response.setMessage("precheck failed");
        response.getData().setCanTrade(false);
        response.getData().addReason(reason);
        return response;
    }

    public int getCode() {
        return code;
    }

    public void setCode(int code) {
        this.code = code;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public PreCheckData getData() {
        return data;
    }

    public void setData(PreCheckData data) {
        this.data = data;
    }

    /**
     * Inner data class containing check result
     */
    public static class PreCheckData {
        private boolean canTrade;
        private List<String> reasons = new ArrayList<>();

        public boolean isCanTrade() {
            return canTrade;
        }

        public void setCanTrade(boolean canTrade) {
            this.canTrade = canTrade;
        }

        public List<String> getReasons() {
            return reasons;
        }

        public void setReasons(List<String> reasons) {
            this.reasons = reasons;
        }

        public void addReason(String reason) {
            this.reasons.add(reason);
        }
    }
}