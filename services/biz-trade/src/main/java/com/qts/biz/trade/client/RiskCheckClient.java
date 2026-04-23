package com.qts.biz.trade.client;

import com.qts.biz.risk.grpc.RiskCheckRequestProto;
import com.qts.biz.risk.grpc.RiskCheckResponseProto;
import com.qts.biz.risk.grpc.RiskServiceGrpc;
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
    private final RiskServiceGrpc riskServiceGrpc;

    @Autowired
    public RiskCheckClient(ManagedChannel channel, RiskConfig riskConfig) {
        this.channel = channel;
        this.riskConfig = riskConfig;
        this.riskServiceGrpc = new RiskServiceGrpc(channel);
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
            // Build the gRPC request
            RiskCheckRequestProto request = RiskCheckRequestProto.newBuilder()
                    .setAccountId(String.valueOf(accountId))
                    .setSymbol(symbol)
                    .setSide(side)
                    .setPrice(price != null ? price : 0.0)
                    .setQuantity(quantity != null ? quantity.intValue() : 0)
                    .setOrderAmount(price != null && quantity != null ? price * quantity : 0.0)
                    .build();

            // Make the actual gRPC call to risk-service
            RiskCheckResponseProto response = riskServiceGrpc.checkRisk(request);

            // Map the response to our internal result
            RiskCheckResult result = new RiskCheckResult();
            result.setPassed(response.isPassed());
            result.setAccountId(accountId);
            result.setMessage(response.getMessage());
            result.setRejectCode(response.getRejectCode());
            
            logger.info("Risk check completed for accountId={}: passed={}", 
                       accountId, response.isPassed());
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
