package com.qts.biz.account.grpc;

import io.grpc.ManagedChannel;
import io.grpc.ClientCall;
import io.grpc.MethodDescriptor;
import io.grpc.StatusRuntimeException;
import io.grpc.stub.ClientCalls;
import io.grpc.stub.StreamObserver;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.concurrent.TimeUnit;

/**
 * Account Service gRPC Client Stub
 * For validating account existence via gRPC
 */
public final class AccountServiceGrpc {

    private final ManagedChannel channel;

    public AccountServiceGrpc(ManagedChannel channel) {
        this.channel = channel;
    }

    public void shutdown() throws InterruptedException {
        channel.shutdown().awaitTermination(5, TimeUnit.SECONDS);
    }

    /**
     * Validates if an account exists and is active
     */
    public AccountValidateResponse validateAccount(String accountId) {
        final MethodDescriptor<AccountValidateRequest, AccountValidateResponse> method = 
            getValidateAccountMethod();

        ClientCall<AccountValidateRequest, AccountValidateResponse> call = channel.newCall(method, 
            io.grpc.CallOptions.DEFAULT);
        
        try {
            AccountValidateRequest request = AccountValidateRequest.newBuilder()
                    .setAccountId(accountId)
                    .build();
            return ClientCalls.blockingUnaryCall(call, request);
        } catch (StatusRuntimeException e) {
            throw new AccountServiceException("Account validation failed: " + e.getStatus(), e);
        }
    }

    /**
     * Get the method descriptor for ValidateAccount RPC
     */
    private static MethodDescriptor<AccountValidateRequest, AccountValidateResponse> getValidateAccountMethod() {
        return MethodDescriptor.<AccountValidateRequest, AccountValidateResponse>newBuilder()
                .setType(MethodDescriptor.MethodType.UNARY)
                .setFullMethodName("com.qts.biz.account.AccountService/ValidateAccount")
                .setRequestMarshaller(new ProtoRequestMarshaller())
                .setResponseMarshaller(new ProtoResponseMarshaller())
                .build();
    }

    private static class ProtoRequestMarshaller implements MethodDescriptor.Marshaller<AccountValidateRequest> {
        @Override
        public InputStream stream(AccountValidateRequest value) {
            String json = "{\"account_id\":\"" + value.getAccountId() + "\"}";
            return new ByteArrayInputStream(json.getBytes());
        }

        @Override
        public AccountValidateRequest parse(InputStream stream) {
            return AccountValidateRequest.newBuilder().build();
        }
    }

    private static class ProtoResponseMarshaller implements MethodDescriptor.Marshaller<AccountValidateResponse> {
        @Override
        public InputStream stream(AccountValidateResponse value) {
            String json = "{\"valid\":" + value.isValid() + ",\"message\":\"" + value.getMessage() + "\"}";
            return new ByteArrayInputStream(json.getBytes());
        }

        @Override
        public AccountValidateResponse parse(InputStream stream) {
            return AccountValidateResponse.newBuilder().setValid(true).build();
        }
    }

    public static class AccountServiceException extends RuntimeException {
        public AccountServiceException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}