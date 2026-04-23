package com.qts.biz.risk.precheck.service.impl;

import com.qts.biz.risk.precheck.service.AssetService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

/**
 * Mock implementation of AssetService
 * In production, this would call external asset service/database
 */
@Service
public class MockAssetServiceImpl implements AssetService {

    private static final Logger logger = LoggerFactory.getLogger(MockAssetServiceImpl.class);

    // Mock data storage: accountId -> asset info
    private final Map<Long, AssetInfo> assetData = new HashMap<>();

    public MockAssetServiceImpl() {
        // Initialize with default mock data
        logger.info("MockAssetService initialized");
    }

    @Override
    public BigDecimal getTotalAssets(Long accountId) {
        logger.debug("Getting total assets for accountId={}", accountId);
        AssetInfo info = assetData.get(accountId);
        return info != null ? info.totalAssets : new BigDecimal("1000000"); // Default 100万
    }

    @Override
    public BigDecimal getAvailableCash(Long accountId) {
        logger.debug("Getting available cash for accountId={}", accountId);
        AssetInfo info = assetData.get(accountId);
        return info != null ? info.availableCash : new BigDecimal("500000"); // Default 50万
    }

    /**
     * Update asset info (for testing purposes)
     */
    public void updateAsset(Long accountId, BigDecimal totalAssets, BigDecimal availableCash) {
        assetData.put(accountId, new AssetInfo(totalAssets, availableCash));
    }

    private static class AssetInfo {
        final BigDecimal totalAssets;
        final BigDecimal availableCash;

        AssetInfo(BigDecimal totalAssets, BigDecimal availableCash) {
            this.totalAssets = totalAssets;
            this.availableCash = availableCash;
        }
    }
}