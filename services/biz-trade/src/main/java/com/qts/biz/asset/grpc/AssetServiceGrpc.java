package com.qts.biz.asset.grpc;

import io.grpc.ManagedChannel;
import io.grpc.ClientCall;
import io.grpc.MethodDescriptor;
import io.grpc.StatusRuntimeException;
import io.grpc.stub.ClientCalls;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.concurrent.TimeUnit;

/**
 * Asset Service gRPC Client Stub
 * For checking fund sufficiency via gRPC
 */
public final class AssetServiceGrpc {

    private final ManagedChannel channel;

    public AssetServiceGrpc(ManagedChannel channel) {
        this.channel = channel;
    }

    public void shutdown() throws InterruptedException {
        channel.shutdown().awaitTermination(5, TimeUnit.SECONDS);
    }

    /**
     * Checks if the account has sufficient funds/positions for the order
     */
    public AssetCheckResponse checkSufficiency(String accountId, String symbol, String side, 
                                                double amount, int quantity) {
        final MethodDescriptor<AssetCheckRequest, AssetCheckResponse> method = 
            getCheckSufficiencyMethod();

        ClientCall<AssetCheckRequest, AssetCheckResponse> call = channel.newCall(method, 
            io.grpc.CallOptions.DEFAULT);
        
        try {
            AssetCheckRequest request = AssetCheckRequest.newBuilder()
                    .setAccountId(accountId)
                    .setSymbol(symbol)
                    .setSide(side)
                    .setAmount(amount)
                    .setQuantity(quantity)
                    .build();
            return ClientCalls.blockingUnaryCall(call, request);
        } catch (StatusRuntimeException e) {
            throw new AssetServiceException("Asset check failed: " + e.getStatus(), e);
        }
    }

    /**
     * Get the method descriptor for CheckSufficiency RPC
     */
    private static MethodDescriptor<AssetCheckRequest, AssetCheckResponse> getCheckSufficiencyMethod() {
        return MethodDescriptor.<AssetCheckRequest, AssetCheckResponse>newBuilder()
                .setType(MethodDescriptor.MethodType.UNARY)
                .setFullMethodName("com.qts.biz.asset.AssetService/CheckSufficiency")
                .setRequestMarshaller(new ProtoRequestMarshaller())
                .setResponseMarshaller(new ProtoResponseMarshaller())
                .build();
    }

    private static class ProtoRequestMarshaller implements MethodDescriptor.Marshaller<AssetCheckRequest> {
        @Override
        public InputStream stream(AssetCheckRequest value) {
            String json = String.format(
                "{\"account_id\":\"%s\",\"symbol\":\"%s\",\"side\":\"%s\",\"amount\":%f,\"quantity\":%d}",
                value.getAccountId(), value.getSymbol(), value.getSide(),
                value.getAmount(), value.getQuantity()
            );
            return new ByteArrayInputStream(json.getBytes());
        }

        @Override
        public AssetCheckRequest parse(InputStream stream) {
            return AssetCheckRequest.newBuilder().build();
        }
    }

    private static class ProtoResponseMarshaller implements MethodDescriptor.Marshaller<AssetCheckResponse> {
        @Override
        public InputStream stream(AssetCheckResponse value) {
            String json = String.format(
                "{\"sufficient\":%b,\"message\":\"%s\",\"available_cash\":%f,\"available_position\":%f}",
                value.isSufficient(), value.getMessage(),
                value.getAvailableCash(), value.getAvailablePosition()
            );
            return new ByteArrayInputStream(json.getBytes());
        }

        @Override
        public AssetCheckResponse parse(InputStream stream) {
            return AssetCheckResponse.newBuilder().setSufficient(true).build();
        }
    }

    public static class AssetServiceException extends RuntimeException {
        public AssetServiceException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}