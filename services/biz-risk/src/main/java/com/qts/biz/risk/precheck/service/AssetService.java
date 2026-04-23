package com.qts.biz.risk.precheck.service;

import java.math.BigDecimal;

/**
 * Asset Service Interface
 * Provides asset/cash data for risk validation
 */
public interface AssetService {

    /**
     * Get total assets for an account
     * @param accountId Account ID
     * @return Total assets value
     */
    BigDecimal getTotalAssets(Long accountId);

    /**
     * Get available cash for trading
     * @param accountId Account ID
     * @return Available cash amount
     */
    BigDecimal getAvailableCash(Long accountId);
}