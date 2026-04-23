package com.qts.biz.asset.grpc;

import io.grpc.ManagedChannel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Asset Service gRPC Client
 * Checks fund sufficiency for orders
 */
@Component
public class AssetServiceClient {

    private static final Logger logger = LoggerFactory.getLogger(AssetServiceClient.class);

    private final ManagedChannel channel;
    private final AssetServiceGrpc assetServiceGrpc;

    @Autowired
    public AssetServiceClient(ManagedChannel assetChannel) {
        this.channel = assetChannel;
        this.assetServiceGrpc = new AssetServiceGrpc(channel);
    }

    /**
     * Checks if the account has sufficient funds/positions for the order
     * For BUY orders: checks if sufficient cash
     * For SELL orders: checks if sufficient position
     * 
     * @param accountId Account ID
     * @param symbol Trading symbol
     * @param side Order side (BUY/SELL)
     * @param amount Order amount (price * quantity)
     * @param quantity Order quantity
     * @return Check result with sufficiency status and details
     */
    public AssetCheckResult checkSufficiency(String accountId, String symbol, String side,
                                             double amount, int quantity) {
        logger.info("Checking asset sufficiency for account={}, symbol={}, side={}, amount={}, qty={}",
                   accountId, symbol, side, amount, quantity);
        
        try {
            AssetCheckResponse response = assetServiceGrpc.checkSufficiency(
                accountId, symbol, side, amount, quantity);
            
            AssetCheckResult result = new AssetCheckResult();
            result.setSufficient(response.isSufficient());
            result.setMessage(response.getMessage());
            result.setAvailableCash(response.getAvailableCash());
            result.setAvailablePosition(response.getAvailablePosition());
            
            logger.info("Asset check result for {}: sufficient={}", accountId, response.isSufficient());
            return result;
        } catch (Exception e) {
            logger.error("Asset check failed for {}: {}", accountId, e.getMessage());
            AssetCheckResult result = new AssetCheckResult();
            result.setSufficient(false);
            result.setMessage("Asset check error: " + e.getMessage());
            return result;
        }
    }

    /**
     * Result DTO for asset sufficiency check
     */
    public static class AssetCheckResult {
        private boolean sufficient;
        private String message;
        private double availableCash;
        private double availablePosition;

        public boolean isSufficient() {
            return sufficient;
        }

        public void setSufficient(boolean sufficient) {
            this.sufficient = sufficient;
        }

        public String getMessage() {
            return message;
        }

        public void setMessage(String message) {
            this.message = message;
        }

        public double getAvailableCash() {
            return availableCash;
        }

        public void setAvailableCash(double availableCash) {
            this.availableCash = availableCash;
        }

        public double getAvailablePosition() {
            return availablePosition;
        }

        public void setAvailablePosition(double availablePosition) {
            this.availablePosition = availablePosition;
        }
    }
}