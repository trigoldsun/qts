package com.qts.biz.trade.client;

import com.qts.biz.trade.config.RiskConfig;
import io.grpc.ManagedChannel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

/**
 * Risk Check gRPC Client
 * Handles pre-trade risk validation by communicating with BIZ-RISK service
 */
@Component
public class RiskCheckClient {

    private static final Logger logger = LoggerFactory.getLogger(RiskCheckClient.class);

    private final ManagedChannel channel;
    private final RiskConfig riskConfig;

    @Autowired
    public RiskCheckClient(ManagedChannel channel, RiskConfig riskConfig) {
        this.channel = channel;
        this.riskConfig = riskConfig;
    }

    /**
     * Perform pre-trade risk check
     * @param accountId Account ID for risk evaluation
     * @param symbol Trading symbol
     * @param side Order side (BUY/SELL)
     * @param price Order price
     * @param quantity Order quantity
     * @return Risk check result
     */
    public RiskCheckResult checkRisk(Long accountId, String symbol, String side, 
                                      Double price, Double quantity) {
        logger.info("Performing risk check for accountId={}, symbol={}, side={}", 
                    accountId, symbol, side);
        
        try {
            // TODO: Implement actual gRPC call to risk-service
            // For now, return a permissive result
            RiskCheckResult result = new RiskCheckResult();
            result.setPassed(true);
            result.setAccountId(accountId);
            result.setMessage("Risk check passed");
            return result;
        } catch (Exception e) {
            logger.error("Risk check failed for accountId={}: {}", accountId, e.getMessage());
            RiskCheckResult result = new RiskCheckResult();
            result.setPassed(false);
            result.setAccountId(accountId);
            result.setMessage("Risk check error: " + e.getMessage());
            return result;
        }
    }

    /**
     * Shutdown the gRPC channel gracefully
     */
    public void shutdown() {
        try {
            channel.shutdown().awaitTermination(riskConfig.getTimeoutMs(), TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            logger.warn("RiskCheckClient shutdown interrupted");
            channel.shutdownNow();
        }
    }

    /**
     * Risk Check Result DTO
     */
    public static class RiskCheckResult {
        private boolean passed;
        private Long accountId;
        private String message;
        private String rejectCode;

        public boolean isPassed() {
            return passed;
        }

        public void setPassed(boolean passed) {
            this.passed = passed;
        }

        public Long getAccountId() {
            return accountId;
        }

        public void setAccountId(Long accountId) {
            this.accountId = accountId;
        }

        public String getMessage() {
            return message;
        }

        public void setMessage(String message) {
            this.message = message;
        }

        public String getRejectCode() {
            return rejectCode;
        }

        public void setRejectCode(String rejectCode) {
            this.rejectCode = rejectCode;
        }
    }
}
