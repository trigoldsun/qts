package com.qts.biz.account.grpc;

import io.grpc.ManagedChannel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Account Service gRPC Client
 * Validates account existence and status
 */
@Component
public class AccountServiceClient {

    private static final Logger logger = LoggerFactory.getLogger(AccountServiceClient.class);

    private final ManagedChannel channel;
    private final AccountServiceGrpc accountServiceGrpc;

    @Autowired
    public AccountServiceClient(ManagedChannel accountChannel) {
        this.channel = accountChannel;
        this.accountServiceGrpc = new AccountServiceGrpc(channel);
    }

    /**
     * Validates if an account exists and is active
     * @param accountId Account ID to validate
     * @return Validation result with success status and message
     */
    public AccountValidationResult validateAccount(String accountId) {
        logger.info("Validating account: {}", accountId);
        
        try {
            AccountValidateResponse response = accountServiceGrpc.validateAccount(accountId);
            
            AccountValidationResult result = new AccountValidationResult();
            result.setValid(response.isValid());
            result.setMessage(response.getMessage());
            
            logger.info("Account validation result for {}: valid={}", accountId, response.isValid());
            return result;
        } catch (Exception e) {
            logger.error("Account validation failed for {}: {}", accountId, e.getMessage());
            AccountValidationResult result = new AccountValidationResult();
            result.setValid(false);
            result.setMessage("Account validation error: " + e.getMessage());
            return result;
        }
    }

    /**
     * Result DTO for account validation
     */
    public static class AccountValidationResult {
        private boolean valid;
        private String message;

        public boolean isValid() {
            return valid;
        }

        public void setValid(boolean valid) {
            this.valid = valid;
        }

        public String getMessage() {
            return message;
        }

        public void setMessage(String message) {
            this.message = message;
        }
    }
}